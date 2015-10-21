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
 * A big model specification
 */
class BigModel[K : ClassTag, V : ClassTag](partitioner: Partitioner[K, ActorRef], default: V) {

  /**
   * Asynchronous function to pull values from the big model
   *
   * @param keys The array of keys
   * @return A future for the array of returned values
   */
  def pull(keys: Array[K]): Future[Array[V]] = {
    implicit val ec = ExecutionContext.Implicits.global

    // Send pull request of the list of keys
    val pulls = keys.groupBy(partitioner.partition).map {
      case (partition, partitionKeys) =>
        ask(partition, Pull[K](partitionKeys))(Timeout(30 seconds)).mapTo[Response[V]]
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
   * Asynchronous function to push values to the big model
   *
   * @param keys The array of keys
   * @param values The array of values
   * @return A future for the completion of the operation
   */
  def push(keys: Array[K], values: Array[V]): Future[Unit] = {
    implicit val ec = ExecutionContext.Implicits.global

    // Send push request
    val pushes = keys.zip(values).groupBy{
      case (k,v) => partitioner.partition(k)
    }.map {
      case (partition, keyValuePairs) =>
        ask(partition, Push[K, V](keyValuePairs.map(_._1), keyValuePairs.map(_._2)))(Timeout(30 seconds))
    }

    // Combine and aggregate futures
    Future.sequence(pushes).transform(results => Unit, err => err)
  }

}
