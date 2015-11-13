package glint.models.cache

import glint.models.BigModel

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import spray.caching.{Cache, LruCache}

import scala.reflect.ClassTag

/**
  * A Cache trait for models
  */
class CacheModel[K: ClassTag, V: ClassTag](bigModel: BigModel[K, V],
                                           aggregate: (V, V) => V,
                                           val pushQueueSize: Int = 200) {

  val cache: Cache[V] = LruCache()
  val pushQueue = mutable.HashMap[K, V]()

  /**
    * Pushes a (key, value) pair asynchronously
    *
    * @param key The key
    * @param value The value
    */
  def push(key: K, value: V)(implicit ec: ExecutionContext): Unit = {
    pushQueue(key) = aggregate(pushQueue.getOrElse(key, bigModel.default), value)
    if (pushQueue.keySet.size > pushQueueSize) {
      flush()
    }
  }

  /**
    * Pulls a (key, value) pair asynchronously
    *
    * @param key The key
    * @return A future holding the value (could be returned from cache)
    */
  def pull(key: K)(implicit ec: ExecutionContext): Future[V] = cache(key) {
    bigModel.pullSingle(key)
  }

  /**
    * Runs all pending operations
    *
    * @return A future for the completion of the pending operations
    */
  def flush()(implicit ec: ExecutionContext): Future[Unit] = {
    val keys = pushQueue.keys.toArray
    val values = keys.map(k => pushQueue(k))
    pushQueue.clear()
    println(s"Flushing keys: [${keys.mkString(",")}]")
    bigModel.push(keys, values)
  }

}
