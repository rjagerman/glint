package glint.models.client

import java.util.concurrent.Executors

import spray.caching.{Cache, LruCache}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.ClassTag

/**
  * A cached BigModel wrapper
  */
class CachedBigModel[K: ClassTag, V: ClassTag](val bigModel: BigModel[K, V],
                                               aggregate: (V, V) => V,
                                               val pushQueueSize: Int = 200,
                                               val pullCacheSize: Int = 200,
                                               val timeToLive: Duration = 60 seconds,
                                               val timeToIdle: Duration = 59 seconds) extends BigModel[K, V] {

  private val cache: Cache[V] = LruCache(pullCacheSize, pullCacheSize, timeToLive, timeToIdle)
  private val pushQueue = mutable.HashMap[K, V]()
  private val promisePushes = mutable.Set[Promise[Unit]]()
  private val updateEc = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /**
    * Function to pull values from the big model
    *
    * @param keys The array of keys
    * @return A future for the array of returned values
    */
  override def pull(keys: Array[K])(implicit ec: ExecutionContext): Future[Array[V]] = {
    val futures = keys.indices.map(i => pullSingle(keys(i)))
    Future.sequence(futures).transform(values => values.toArray, err => err)
  }

  /**
    * Function to push values to the big model
    *
    * @param keys The array of keys
    * @param values The array of values
    * @return A future for the completion of the operation
    */
  override def push(keys: Array[K], values: Array[V])(implicit ec: ExecutionContext): Future[Unit] = {
    val futures = keys.indices.map(i => pushSingle(keys(i), values(i)))
    Future.sequence(futures).transform(s => (), err => err)
  }

  /**
    * Function to pull a single value from the big model
    *
    * @param key The key
    * @return A future for the returned value
    */
  override def pullSingle(key: K)(implicit ec: ExecutionContext): Future[V] = cache(key) {
    bigModel.pullSingle(key)
  }

  /**
    * Function to push a single value to the big model
    *
    * @param key The key
    * @return A future for the completion of the operation
    */
  override def pushSingle(key: K, value: V)(implicit ec: ExecutionContext): Future[Unit] = {
    val promise = Promise[Unit]()
    Future {
      promisePushes += promise
      if (pushQueue.contains(key)) {
        pushQueue(key) = aggregate(pushQueue(key), value)
      } else {
        pushQueue(key) = value
      }
      if (pushQueue.keySet.size > pushQueueSize) {
        val (ks, vs, ps) = getPushesAndClear
        flush(ks, vs, ps)
      }
    }(updateEc)
    promise future
  }

  /**
    * Destroys the model and releases the resources at the parameter servers
    */
  override def destroy(): Unit = bigModel.destroy()

  /**
    * Flushes currently cached pushed requests to the parameter servers
    *
    * @return A future for the completion of the operation
    */
  def flush(implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      getPushesAndClear
    }(updateEc).flatMap {
      case (keys, values, promises) => flush(keys, values, promises)
    }
  }

  /**
    * Flushes immediately with given keys, values and promises
    * @param keys The keys
    * @param values The values
    * @param promises The promises to complete
    */
  private def flush(keys: Array[K],
                    values: Array[V],
                    promises: List[Promise[Unit]])(implicit ec: ExecutionContext): Future[Unit] = {
    val future = bigModel.push(keys, values)
    future.onFailure {
      case ex => promises.foreach { case p => p failure ex }
    }
    future.onSuccess {
      case _ => promises.foreach { case p => p success None }
    }
    future
  }

  /**
    * Returns the current push queue and clears it
    *
    * @return The current push queue state
    */
  private def getPushesAndClear: (Array[K], Array[V], List[Promise[Unit]]) = {
    val keys = pushQueue.keys.toArray
    val values = keys.map(k => pushQueue(k))
    val promisesToComplete = promisePushes.toList
    promisePushes.clear()
    pushQueue.clear()
    (keys, values, promisesToComplete)
  }

  /**
    * Number of requests currently processing
    *
    * @return The number of currently procesing requests
    */
  override def processing: Int = bigModel.processing

}

object CachedBigModel {
  def apply[K: ClassTag, V: ClassTag](bigModel: BigModel[K, V],
                                      aggregate: (V, V) => V,
                                      pushQueueSize: Int = 200,
                                      pullCacheSize: Int = 200,
                                      timeToLive: Duration = 60 seconds,
                                      timeToIdle: Duration = 59 seconds): CachedBigModel[K, V] = {
    new CachedBigModel[K, V](bigModel, aggregate, pushQueueSize, pullCacheSize, timeToLive, timeToIdle)
  }
}
