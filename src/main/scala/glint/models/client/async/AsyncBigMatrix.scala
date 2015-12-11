package glint.models.client.async

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import akka.pattern.ask
import akka.serialization.JavaSerializer
import akka.util.Timeout
import breeze.linalg.{DenseVector, Vector}
import breeze.math.Semiring
import com.typesafe.config.Config
import glint.indexing.Indexer
import glint.messages.server.request.{PullMatrix, PullMatrixRows}
import glint.models.client.BigMatrix
import glint.partitioning.Partitioner

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * An asynchronous implementation of the BigMatrix.
  *
  * @param partitioner A partitioner to map rows to parameter servers
  * @param indexer An indexer to remap rows
  * @param config The glint configuration (used for serialization/deserialization construction of actorsystems)
  * @param rows The number of rows
  * @param cols The number of columns
  * @tparam V The type of values to store
  * @tparam R The type of responses we expect to get from the parameter servers
  * @tparam P The type of push requests we should send to the parameter servers
  */
abstract class AsyncBigMatrix[@specialized V: Semiring : ClassTag, R: ClassTag, P: ClassTag](partitioner: Partitioner[ActorRef],
                                                                                             indexer: Indexer[Long],
                                                                                             config: Config,
                                                                                             rows: Long,
                                                                                             cols: Int)
  extends BigMatrix[V] {

  /**
    * Creates a vector from range [start, end) in values in given response
    *
    * @param response The response containing the values
    * @param start The start index
    * @param end The end index
    * @return A vector for the range [start, end)
    */
  protected def toVector(response: R, start: Int, end: Int): Vector[V]

  /**
    * Extracts a value from a given response at given index
    *
    * @param response The response
    * @param index The index
    * @return The value
    */
  protected def toValue(response: R, index: Int): V

  /**
    * Creates a push message from given sequence of rows, columns and values
    *
    * @param rows The rows
    * @param cols The columns
    * @param values The values
    * @return A PushMatrix message for type V
    */
  protected def toPushMessage(rows: Array[Long], cols: Array[Int], values: Array[V]): P

  /**
    * Pulls a set of rows
    *
    * @param rows The indices of the rows
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[Vector[V]]] = {

    // Reindex keys appropriately
    val indexedRows = rows.map(indexer.index)

    // Send pull request of the list of keys
    val pulls = indexedRows.groupBy(partitioner.partition).map {
      case (partition, partitionKeys) =>
        (partition ? PullMatrixRows(partitionKeys)).mapTo[R]
    }

    // Obtain key indices after partitioning so we can place the results in a correctly ordered array
    val indices = indexedRows.zipWithIndex.groupBy {
      case (k, i) => partitioner.partition(k)
    }.map {
      case (_, arr) => arr.map(_._2)
    }

    // Define aggregator for successful responses
    def aggregateSuccess(responses: Iterable[R]): Array[Vector[V]] = {
      val result: Array[Vector[V]] = Array.fill[Vector[V]](rows.length)(DenseVector.zeros[V](0))
      responses.zip(indices).foreach {
        case (response, idx) =>
          for (i <- idx.indices) {
            result(idx(i)) = toVector(response, i * cols, (i + 1) * cols)
          }
      }
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
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long], cols: Array[Int])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]] = {

    // Reindex keys appropriately
    val indexedRows = rows.map(indexer.index)

    // Send pull request of the list of keys
    val pulls = mapPartitions(indexedRows) {
      case (partition, indices) =>
        val pullMessage = PullMatrix(indices.map(indexedRows).toArray, indices.map(cols).toArray)
        (partition ? pullMessage).mapTo[R]
    }

    // Obtain key indices after partitioning so we can place the results in a correctly ordered array
    val indices = indexedRows.zipWithIndex.groupBy {
      case (k, i) => partitioner.partition(k)
    }.map {
      case (_, arr) => arr.map(_._2)
    }

    // Define aggregator for successful responses
    def aggregateSuccess(responses: Iterable[R]): Array[V] = {
      val result = DenseVector.zeros[V](rows.length)
      responses.zip(indices).foreach {
        case (response, idx) =>
          for (i <- idx.indices) {
            result(idx(i)) = toValue(response, i)
          }
      }
      result.toArray
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
    * @return A future containing either the success or failure of the operation
    */
  override def push(rows: Array[Long], cols: Array[Int], values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {

    // Reindex rows appropriately
    val indexedRows = rows.map(indexer.index)

    // Send push requests
    val pushes = mapPartitions(indexedRows) {
      case (partition, indices) =>
        (partition ? toPushMessage(indices.map(rows).toArray, indices.map(cols).toArray, indices.map(values).toArray)).mapTo[Boolean]
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
  private def mapPartitions[T](rows: Seq[Long])(func: (ActorRef, Seq[Int]) => T): Iterable[T] = {
    rows.indices.groupBy(i => partitioner.partition(rows(i))).map { case (a, b) => func(a, b) }
  }

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
