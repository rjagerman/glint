package glint.models

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import glint.indexing.Indexer
import glint.messages.server.{Pull, Push, Response}
import glint.partitioning.Partitioner

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A big model
  *
  * @param partitioner The partitioner used to partition keys into servers
  * @param indexer The indexer used to convert keys to indices
  * @param chunks The partial models
  * @param default The default value
  * @tparam K The type of keys to store
  * @tparam V The type of values to store
  */
class BigModel[K: ClassTag, V: ClassTag](partitioner: Partitioner[ActorRef],
                                         indexer: Indexer[K],
                                         chunks: Array[ActorRef],
                                         default: V) extends Serializable {

  @transient
  private implicit lazy val ec = ExecutionContext.Implicits.global

  @transient
  private implicit lazy val timeout = Timeout(30 seconds)

  /**
    * Asynchronous function to pull values from the big model
    *
    * @param keys The array of keys
    * @return A future for the array of returned values
    */
  def pull(keys: Array[K]): Future[Array[V]] = {

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
    Future.sequence(pulls).transform(aggregateSuccess, err => err)
  }

  /**
    * Asynchronous function to pull a single value from the big model
    *
    * @param key The key
    * @return A future for the returned value
    */
  def pullSingle(key: K): Future[V] = {
    pull(Array(key)).map { case values: Array[V] => values(0) }
  }

  /**
    * Asynchronous function to push values to the big model
    *
    * @param keys The array of keys
    * @return A future for the completion of the operation
    */
  def push(keys: Array[K], values: Array[V]): Future[Unit] = {

    // Reindex keys appropriately
    val indexedKeys = keys.map(indexer.index)

    // Send push request
    val pushes = indexedKeys.groupBy(k => partitioner.partition(k)).map {
      case (partition, keys) => partition ? Push[Long, V](keys, values)
    }

    // Combine and aggregate futures
    Future.sequence(pushes).transform(results => Unit, err => err)
  }

  /**
    * Asynchronous function to push a single value to the big model
    *
    * @param key The key
    * @return A future for the completion of the operation
    */
  def pushSingle(key: K, value: V): Future[Unit] = {
    push(Array(key), Array(value))
  }

  /**
    * Destroys the model and releases the resources at the parameter servers
    */
  def destroy(): Unit = {
    chunks.foreach {
      case chunk => chunk ! akka.actor.PoisonPill
    }
  }

}
