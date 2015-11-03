package glint.models

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import glint.messages.server.{Push, Response, Pull}
import glint.partitioning.Partitioner
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * A big model
 */
class BigModel[K : ClassTag, V : ClassTag](partitioner: Partitioner[K, ActorRef], chunks: Array[ActorRef], default: V)
  extends Serializable {

  /**
   * Asynchronous function to pull values from the big model
   *
   * @param keys The array of keys
   * @return A future for the array of returned values
   */
  def pull(keys: Array[K]): Future[Array[V]] = {
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(30 seconds)

    // Send pull request of the list of keys
    val pulls = keys.groupBy(partitioner.partition).map {
      case (partition, partitionKeys) =>
        (partition ? Pull[K](partitionKeys)).mapTo[Response[V]]
    }

    // Obtain key indices after partitioning so we can place the results in a correctly ordered array
    val indices = keys.zipWithIndex.groupBy {
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
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(30 seconds)
    val server = partitioner.partition(key)
    (server ? Pull[K](Array(key))).mapTo[Response[V]].map(r => r.values(0))
  }

  /**
   * Asynchronous function to push values to the big model
   *
   * @param keys The array of keys
   * @param values The array of values
   * @return A future for the completion of the operation
   */
  def push(keys: Array[K], values: Array[V]): Future[Unit] = {
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(30 seconds)

    // Send push request
    val pushes = keys.zip(values).groupBy{
      case (k,v) => partitioner.partition(k)
    }.map {
      case (partition, keyValuePairs) =>
        partition ? Push[K, V](keyValuePairs.map(_._1), keyValuePairs.map(_._2))
    }

    // Combine and aggregate futures
    Future.sequence(pushes).transform(results => Unit, err => err)
  }

  /**
   * Asynchronous function to push a single value to the big model
   *
   * @param key The key
   * @param value The value
   * @return A future for the completion of the operation
   */
  def pushSingle(key: K, value: V): Future[Unit] = {
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(30 seconds)
    val server = partitioner.partition(key)
    (server ? Push[K, V](Array(key), Array(value))).transform(results => Unit, err => err)
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
