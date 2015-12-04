package glint.models.client

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import glint.indexing.Indexer
import glint.messages.server.{Pull, Push, Response}
import glint.partitioning.Partitioner

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * An asynchronous implementation of a BigModel
  *
  * @param partitioner A partitioner that partitions keys into partial model references
  * @param indexer An indexer that maps a set of keys to a new set
  * @param chunks References to the partial models
  * @param default The default value to store in this model
  * @tparam K The type of key
  * @tparam V The type of value
  */
class AsyncBigModel[K: ClassTag, V: ClassTag](partitioner: Partitioner[ActorRef],
                                              indexer: Indexer[K],
                                              chunks: Array[ActorRef],
                                              val default: V) extends BigModel[K, V] {

  private val processingPulls: AtomicInteger = new AtomicInteger()
  private val processingPushes: AtomicInteger = new AtomicInteger()

  @transient
  private implicit lazy val timeout = Timeout(30 seconds)

  /**
    * Function to pull values from the big model
    *
    * @param keys The sequence of keys
    * @return A future for the sequence of returned values
    */
  override def pull(keys: Array[K])(implicit ec: ExecutionContext): Future[Array[V]] = {

    // Increment currently in process tasks
    processingPulls.incrementAndGet()

    Future {

      // Reindex keys appropriately
      val indexedKeys = keys.map(indexer.index)

      // Send pull request of the list of keys
      val pulls = indexedKeys.groupBy(partitioner.partition).map {
        case (partition, partitionKeys) =>
          (partition ? Pull[Long](partitionKeys)).mapTo[Response[V]]
      }

      // Obtain key indices after partitioning so we can place the results in a correctly ordered array
      val indices = indexedKeys.zipWithIndex.groupBy {
        case (k, i) => partitioner.partition(k)
      }.map {
        case (_, arr) => arr.map(_._2)
      }

      // Define aggregator for successfull responses
      def aggregateSuccess(responses: Iterable[Response[V]]): Array[V] = {
        val result = Array.fill[V](keys.length)(default)
        responses.zip(indices).foreach {
          case (response, idx) => idx.zip(response.values).foreach {
            case (k, v) => result(k) = v
          }
        }
        result
      }

      // Combine and aggregate futures
      val allFutures = Future.sequence(pulls).transform(aggregateSuccess, err => err)
      allFutures.andThen {
        case _ => processingPulls.decrementAndGet()
      }
      /*allFutures.onComplete {
        case _ =>
          processingPulls.decrementAndGet()
      }
      allFutures*/

    }.flatMap(identity)

  }

  /**
    * Function to push values to the big model
    *
    * @param keys The array of keys
    * @param values The array of values
    * @return A future for the completion of the operation
    */
  override def push(keys: Array[K], values: Array[V])(implicit ec: ExecutionContext): Future[Unit] = {

    // Increment currently in process tasks
    processingPushes.incrementAndGet()

    Future {

      // Reindex keys appropriately
      val indexedKeys = keys.map(indexer.index)

      // Send push request
      val pushes = indexedKeys.zip(values).groupBy { case (k,v) => partitioner.partition(k) }.map {
        case (partition, keyValues) => partition ? Push[Long, V](keyValues.map(_._1), keyValues.map(_._2))
      }

      // Combine and aggregate futures
      val allFutures: Future[Unit] = Future.sequence(pushes).transform(results => (), err => err)
      allFutures.andThen {
        case _ => processingPushes.decrementAndGet()
      }
      /*allFutures.onComplete {
        case _ =>
          processingPushes.decrementAndGet()
      }
      allFutures*/
    }.flatMap(identity)

  }

  /**
    * Function to pull a single value from the big model
    *
    * @param key The key
    * @return A future for the returned value
    */
  override def pullSingle(key: K)(implicit ec: ExecutionContext): Future[V] = {
    pull(Array(key)).map { case values: Array[V] => values(0) }
  }

  /**
    * Function to push a single value to the big model
    *
    * @param key The key
    * @return A future for the completion of the operation
    */
  override def pushSingle(key: K, value: V)(implicit ec: ExecutionContext): Future[Unit] = {
    push(Array(key), Array(value))
  }

  /**
    * Destroys the model and releases the resources at the parameter servers
    */
  override def destroy(): Unit = {
    chunks.foreach {
      case chunk => chunk ! akka.actor.PoisonPill
    }
  }

  /**
    * Number of requests currently processing
    *
    * @return The number of currently procesing requests
    */
  override def processing: Int = processingPulls.get() + processingPushes.get()

}
