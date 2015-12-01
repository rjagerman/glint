package glint.models.client

import java.util.concurrent.Semaphore

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A BigModel with limited amount of requests through a semaphore-based back-pressure mechanism
  * Note that push and pull requests using this model might be blocking and can result in deadlocks if not carefully
  * used.
  *
  * @param bigModel The underlying big model
  * @param maximum The maximum amount of requests in flight at any given time
  * @tparam K The type of keys
  * @tparam V The type of values
  */
class BoundedBigModel[K: ClassTag, V: ClassTag](val bigModel: BigModel[K, V],
                                                val maximum: Int) extends BigModel[K, V] {

  /**
    * Semaphore to keep track of number of concurrent requests
    */
  val semaphore = new Semaphore(maximum)

  /**
    * Executes a function that returns a future and hooks a semaphore release into the functions onComplete callback
    *
    * @param function The future-generating function to execute
    * @return The function's resulting future with an extra onComplete callback
    */
  private def blockingExecuteFunction[F](function: () => Future[F])(implicit ec: ExecutionContext): Future[F] = {
    semaphore.acquire()
    val future = function()
    future.onComplete { case _ => semaphore.release() }
    future
  }

  /**
    * Function to pull values from the big model
    *
    * @param keys The array of keys
    * @return A future for the array of returned values
    */
  override def pull(keys: Array[K])(implicit ec: ExecutionContext): Future[Array[V]] = {
    blockingExecuteFunction(() => bigModel.pull(keys))
  }

  /**
    * Function to push values to the big model
    *
    * @param keys The array of keys
    * @param values The array of values
    * @return A future for the completion of the operation
    */
  override def push(keys: Array[K], values: Array[V])(implicit ec: ExecutionContext): Future[Unit] = {
    blockingExecuteFunction(() => bigModel.push(keys, values))
  }

  /**
    * Function to pull a single value from the big model
    *
    * @param key The key
    * @return A future for the returned value
    */
  override def pullSingle(key: K)(implicit ec: ExecutionContext): Future[V] = {
    blockingExecuteFunction(() => bigModel.pullSingle(key))
  }

  /**
    * Function to push a single value to the big model
    *
    * @param key The key
    * @return A future for the completion of the operation
    */
  override def pushSingle(key: K, value: V)(implicit ec: ExecutionContext): Future[Unit] = {
    blockingExecuteFunction(() => bigModel.pushSingle(key, value))
  }

  /**
    * Destroys the model and releases the resources at the parameter servers
    */
  override def destroy(): Unit = bigModel.destroy()

  /**
    * Number of requests currently processing
    *
    * @return The number of currently procesing requests
    */
  override def processing: Int = bigModel.processing

}
