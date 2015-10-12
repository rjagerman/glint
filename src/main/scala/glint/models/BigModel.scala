package glint.models

import scala.concurrent.Future

/**
 * A big model specification
 */
trait BigModel[K, V] {

  /**
   * Asynchronous function to pull values from the big model
   *
   * @param keys The array of keys
   * @return A future for the array of returned values
   */
  def pull(keys: Array[K]): Future[Array[V]]

  /**
   * Asynchronous function to push values to the big model
   *
   * @param keys The array of keys
   * @param values The array of values
   * @return A future for the completion of the operation
   */
  def push(keys: Array[K], values: Array[V]): Future[Unit]

}
