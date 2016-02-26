package glint.models.client.async

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem}
import akka.pattern.Patterns.gracefulStop
import akka.pattern.ask
import akka.serialization.JavaSerializer
import akka.util.Timeout
import breeze.linalg.DenseVector
import breeze.math.Semiring
import com.typesafe.config.Config
import glint.messages.server.request.PullVector
import glint.models.client.BigVector
import glint.partitioning.{Partition, Partitioner}
import spire.implicits._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * An asynchronous implementation of the [[glint.models.client.BigVector BigVector]]. You don't want to construct this
  * object manually but instead use the methods provided in [[glint.Client Client]], as so
  * {{{
  *   client.vector[Double](keys)
  * }}}
  *
  * @param partitioner A partitioner to map keys to parameter servers
  * @param models The partial models on the parameter servers
  * @param rows The number of rows
  * @tparam V The type of values to store
  * @tparam R The type of responses we expect to get from the parameter servers
  * @tparam P The type of push requests we should send to the parameter servers
  */
abstract class AsyncBigVector[@specialized V: Semiring : ClassTag, R: ClassTag, P: ClassTag](partitioner: Partitioner,
                                                                                             models: Array[ActorRef],
                                                                                             config: Config,
                                                                                             rows: Long)
  extends BigVector[V] {

  /**
    * Pulls a set of elements
    *
    * @param keys The indices of the keys
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(keys: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]] = {

    // Send pull request of the list of keys
    val pulls = mapPartitions(keys) {
      case (partition, indices) =>
        val pullMessage = PullVector(indices.map(keys).toArray)
        (models(partition.index) ? pullMessage).mapTo[R]
    }

    // Obtain key indices after partitioning so we can place the results in a correctly ordered array
    val indices = keys.zipWithIndex.groupBy {
      case (k, i) => partitioner.partition(k)
    }.map {
      case (_, arr) => arr.map(_._2)
    }.toArray

    // Define aggregator for successful responses
    def aggregateSuccess(responses: Iterable[R]): Array[V] = {
      val responsesArray = responses.toArray
      val result = DenseVector.zeros[V](keys.length)
      cforRange(0 until responsesArray.length)(i => {
        val response = responsesArray(i)
        val idx = indices(i)
        cforRange(0 until idx.length)(j => {
          result(idx(j)) = toValue(response, j)
        })
      })
      result.toArray
    }

    // Combine and aggregate futures
    Future.sequence(pulls).transform(aggregateSuccess, err => err)

  }

  /**
    * Groups indices of given keys into partitions according to this models partitioner and maps each partition to a
    * type T
    *
    * @param keys The keys to partition
    * @param func The function that takes a partition and corresponding indices and creates something of type T
    * @tparam T The type to map to
    * @return An iterable over the partitioned results
    */
  @inline
  private def mapPartitions[T](keys: Seq[Long])(func: (Partition, Seq[Int]) => T): Iterable[T] = {
    keys.indices.groupBy(i => partitioner.partition(keys(i))).map { case (a, b) => func(a, b) }
  }

  /**
    * Pushes a set of values
    *
    * @param keys The indices of the keys
    * @param values The values to update
    * @return A future containing either the success or failure of the operation
    */
  override def push(keys: Array[Long], values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {

    // Send push requests
    val pushes = mapPartitions(keys) {
      case (partition, indices) =>
        (models(partition.index) ? toPushMessage(indices.map(keys).toArray, indices.map(values).toArray)).mapTo[Boolean]
    }

    // Combine and aggregate futures
    Future.sequence(pushes).transform(results => true, err => err)

  }

  /**
    * @return The number of partitions this big vector's data is spread across
    */
  def nrOfPartitions: Int = {
    partitioner.all().length
  }

  /**
    * Destroys the matrix on the parameter servers
    *
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    val partitionFutures = partitioner.all().map {
      case partition => gracefulStop(models(partition.index), 60 seconds)
    }.toIterator
    Future.sequence(partitionFutures).transform(successes => successes.forall(success => success), err => err)
  }

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
    * Creates a push message from given sequence of keys and values
    *
    * @param keys The rows
    * @param values The values
    * @return A PushMatrix message for type V
    */
  @inline
  protected def toPushMessage(keys: Array[Long], values: Array[V]): P

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
