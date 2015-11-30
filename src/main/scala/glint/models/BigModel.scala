package glint.models

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import glint.indexing.Indexer
import glint.messages.server.{Pull, Push, Response}
import glint.partitioning.Partitioner

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._
import scala.concurrent.{Promise, Await, ExecutionContext, Future}
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
                                         val default: V) extends Serializable {


  private val processingPulls: AtomicInteger = new AtomicInteger()
  private val processingPushes: AtomicInteger = new AtomicInteger()

  @transient
  private lazy val lock = new Object()

  @transient
  private implicit lazy val timeout = Timeout(30 seconds)

  /**
    * Asynchronous function to pull values from the big model
    *
    * @param keys The array of keys
    * @return A future for the array of returned values
    */
  def pull(keys: Array[K])(implicit ec: ExecutionContext): Future[Array[V]] = {

    // Increment currently in process tasks
    processingPulls.incrementAndGet()

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
    allFutures.onComplete {
      case _ =>
        processingPulls.decrementAndGet()
        lock.synchronized { lock.notifyAll() }
    }
    allFutures
  }

  /**
    * Asynchronous function to pull a single value from the big model
    *
    * @param key The key
    * @return A future for the returned value
    */
  def pullSingle(key: K)(implicit ec: ExecutionContext): Future[V] = {
    pull(Array(key)).map { case values: Array[V] => values(0) }
  }

  /**
    * Asynchronous function to push values to the big model
    *
    * @param keys The array of keys
    * @return A future for the completion of the operation
    */
  def push(keys: Array[K], values: Array[V])(implicit ec: ExecutionContext): Future[Unit] = {

    // Increment currently in process tasks
    processingPushes.incrementAndGet()

    // Reindex keys appropriately
    val indexedKeys = keys.map(indexer.index)

    // Send push request
    val pushes = indexedKeys.zip(values).groupBy { case (k,v) => partitioner.partition(k) }.map {
      case (partition, keyValues) => partition ? Push[Long, V](keyValues.map(_._1), keyValues.map(_._2))
    }

    // Combine and aggregate futures
    val allFutures: Future[Unit] = Future.sequence(pushes).transform(results => (), err => err)
    allFutures.onComplete {
      case _ =>
        processingPushes.decrementAndGet()
        lock.synchronized { lock.notifyAll() }
    }
    allFutures
  }

  /**
    * Asynchronous function to push a single value to the big model
    *
    * @param key The key
    * @return A future for the completion of the operation
    */
  def pushSingle(key: K, value: V)(implicit ec: ExecutionContext): Future[Unit] = {
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

  /**
    * Blocking call that waits until all currently processing requests are done
    * @return Time spent waiting
    */
  def block(): Long = block(0)

  /**
    * Blocking call that waits until there are at most given amount of processing requests
    *
    * @param tasks The upper bound on active requests
    * @return Time spent waiting
    */
  def block(tasks: Int): Long = {
    val startTime = System.currentTimeMillis()
    lock.synchronized {
      while (processing() > tasks) {
        try {
          lock.wait(5000)
        } catch {
          case ex: InterruptedException => // On interrupt we continue regular execution
        }
      }
    }
    System.currentTimeMillis() - startTime
  }

  /**
    * Gets the number of currently processing tasks
    * @return The number of currently processing tasks
    */
  def processing(): Int = processingPushes.get() + processingPulls.get()

}
