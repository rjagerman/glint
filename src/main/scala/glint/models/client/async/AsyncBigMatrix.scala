package glint.models.client.async

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import akka.pattern.Patterns.gracefulStop
import akka.pattern.ask
import akka.serialization.JavaSerializer
import akka.util.Timeout
import breeze.linalg.{DenseVector, Vector}
import breeze.math.Semiring
import com.typesafe.config.Config
import glint.messages.server.request.{PullMatrix, PullMatrixRows}
import glint.models.client.BigMatrix
import glint.partitioning.{Partition, Partitioner}
import spire.implicits.cforRange

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * An asynchronous implementation of a [[glint.models.client.BigMatrix BigMatrix]]. You don't want to construct this
  * object manually but instead use the methods provided in [[glint.Client Client]], as so
  * {{{
  *   client.matrix[Double](rows, columns)
  * }}}
  *
  * @param partitioner A partitioner to map rows to partitions
  * @param matrices The references to the partial matrices on the parameter servers
  * @param config The glint configuration (used for serialization/deserialization construction of actorsystems)
  * @param rows The number of rows
  * @param cols The number of columns
  * @tparam V The type of values to store
  * @tparam R The type of responses we expect to get from the parameter servers
  * @tparam P The type of push requests we should send to the parameter servers
  */
abstract class AsyncBigMatrix[@specialized V: Semiring : ClassTag, R: ClassTag, P: ClassTag](partitioner: Partitioner,
                                                                                             matrices: Array[ActorRef],
                                                                                             config: Config,
                                                                                             val rows: Long,
                                                                                             val cols: Int)
  extends BigMatrix[V] {

  /**
    * Pulls a set of rows
    *
    * @param rows The indices of the rows
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[Vector[V]]] = {

    // Send pull request of the list of keys
    val pulls = rows.groupBy(partitioner.partition).map {
      case (partition, partitionKeys) =>
        (matrices(partition.index) ? PullMatrixRows(partitionKeys)).mapTo[R]
    }

    // Obtain key indices after partitioning so we can place the results in a correctly ordered array
    val indices = rows.zipWithIndex.groupBy {
      case (k, i) => partitioner.partition(k)
    }.map {
      case (_, arr) => arr.map(_._2)
    }.toArray

    // Define aggregator for successful responses
    def aggregateSuccess(responses: Iterable[R]): Array[Vector[V]] = {
      val result: Array[Vector[V]] = Array.fill[Vector[V]](rows.length)(DenseVector.zeros[V](0))
      val responsesArray = responses.toArray
      cforRange(0 until responsesArray.length)(i => {
        val response = responsesArray(i)
        val idx = indices(i)
        cforRange(0 until idx.length)(i => {
          result(idx(i)) = toVector(response, i * cols, (i + 1) * cols)
        })
      })
      result
    }

    // Combine and aggregate futures
    Future.sequence(pulls).transform(aggregateSuccess, err => err)
  }

  /**
    * Pulls a set of elements
    *
    * @param rows The indices of the rows
    * @param cols The corresponding indices of the columns
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long], cols: Array[Int])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]] = {

    // Send pull request of the list of keys
    val pulls = mapPartitions(rows) {
      case (partition, indices) =>
        val pullMessage = PullMatrix(indices.map(rows).toArray, indices.map(cols).toArray)
        (matrices(partition.index) ? pullMessage).mapTo[R]
    }

    // Obtain key indices after partitioning so we can place the results in a correctly ordered array
    val indices = rows.zipWithIndex.groupBy {
      case (k, i) => partitioner.partition(k)
    }.map {
      case (_, arr) => arr.map(_._2)
    }.toArray

    // Define aggregator for successful responses
    def aggregateSuccess(responses: Iterable[R]): Array[V] = {
      val responsesArray = responses.toArray
      val result = new Array[V](rows.length)
      cforRange(0 until responsesArray.length)(i => {
        val response = responsesArray(i)
        val idx = indices(i)
        cforRange(0 until idx.length)(j => {
          result(idx(j)) = toValue(response, j)
        })
      })
      result
    }

    // Combine and aggregate futures
    Future.sequence(pulls).transform(aggregateSuccess, err => err)

  }

  /**
    * Pushes a set of values
    *
    * @param rows The indices of the rows
    * @param cols The indices of the columns
    * @param values The values to update
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing either the success or failure of the operation
    */
  override def push(rows: Array[Long], cols: Array[Int], values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {

    // Send push requests
    val pushes = mapPartitions(rows) {
      case (partition, indices) =>
        (matrices(partition.index) ? toPushMessage(indices.map(rows).toArray, indices.map(cols).toArray, indices.map(values).toArray)).mapTo[Boolean]
    }

    // Combine and aggregate futures
    Future.sequence(pushes).transform(results => true, err => err)

  }

  /**
    * Groups indices of given rows into partitions according to this models partitioner and maps each partition to a
    * type T
    *
    * @param rows The rows to partition
    * @param func The function that takes a partition and corresponding indices and creates something of type T
    * @tparam T The type to map to
    * @return An iterable over the partitioned results
    */
  @inline
  private def mapPartitions[T](rows: Seq[Long])(func: (Partition, Seq[Int]) => T): Iterable[T] = {
    rows.indices.groupBy(i => partitioner.partition(rows(i))).map { case (a, b) => func(a, b) }
  }

  /**
    * Destroys the matrix on the parameter servers
    *
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    val partitionFutures = partitioner.all().map {
      case partition => gracefulStop(matrices(partition.index), 60 seconds)
    }.toIterator
    Future.sequence(partitionFutures).transform(successes => successes.forall(success => success), err => err)
  }

  /**
    * @return The number of partitions this big matrix's data is spread across
    */
  def nrOfPartitions: Int = {
    partitioner.all().length
  }

  /**
    * Creates a vector from range [start, end) in values in given response
    *
    * @param response The response containing the values
    * @param start The start index
    * @param end The end index
    * @return A vector for the range [start, end)
    */
  @inline
  protected def toVector(response: R, start: Int, end: Int): Vector[V]

  /**
    * Extracts a value from a given response at given index
    *
    * @param response The response
    * @param index The index
    * @return The value
    */
  @inline
  protected def toValue(response: R, index: Int): V

  /**
    * Creates a push message from given sequence of rows, columns and values
    *
    * @param rows The rows
    * @param cols The columns
    * @param values The values
    * @return A PushMatrix message for type V
    */
  @inline
  protected def toPushMessage(rows: Array[Long], cols: Array[Int], values: Array[V]): P

  /**
    * Deserializes this instance. This starts an ActorSystem with appropriate configuration before attempting to
    * deserialize ActorRefs
    *
    * @param in The object input stream
    * @throws java.io.IOException A possible Input/Output exception
    */
  @throws(classOf[IOException])
  private def readObject(in: ObjectInputStream): Unit = {
    val config = in.readObject().asInstanceOf[Config]
    val as = ActorSystem("AsyncBigMatrix", config.getConfig("glint.client"))
    JavaSerializer.currentSystem.withValue(as.asInstanceOf[ExtendedActorSystem]) {
      in.defaultReadObject()
    }
  }

  /**
    * Serializes this instance. This first writes the config before the entire object to ensure we can deserialize with
    * an ActorSystem in place
    *
    * @param out The object output stream
    * @throws java.io.IOException A possible Input/Output exception
    */
  @throws(classOf[IOException])
  private def writeObject(out: ObjectOutputStream): Unit = {
    out.writeObject(config)
    out.defaultWriteObject()
  }

}
