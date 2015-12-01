package glint.models.client

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Abstract trait for a BigModel
  */
abstract class BigModel[K: ClassTag, V: ClassTag] extends Serializable {

  /**
    * Function to pull values from the big model
    *
    * @param keys The array of keys
    * @return A future for the array of returned values
    */
  def pull(keys: Array[K])(implicit ec: ExecutionContext): Future[Array[V]]

  /**
    * Function to push values to the big model
    *
    * @param keys The array of keys
    * @param values The array of values
    * @return A future for the completion of the operation
    */
  def push(keys: Array[K], values: Array[V])(implicit ec: ExecutionContext): Future[Unit]

  /**
    * Function to pull a single value from the big model
    *
    * @param key The key
    * @return A future for the returned value
    */
  def pullSingle(key: K)(implicit ec: ExecutionContext): Future[V]

  /**
    * Function to push a single value to the big model
    *
    * @param key The key
    * @return A future for the completion of the operation
    */
  def pushSingle(key: K, value: V)(implicit ec: ExecutionContext): Future[Unit]

  /**
    * Destroys the model and releases the resources at the parameter servers
    */
  def destroy(): Unit

  /**
    * Number of requests currently processing
    *
    * @return The number of currently procesing requests
    */
  def processing: Int

}
