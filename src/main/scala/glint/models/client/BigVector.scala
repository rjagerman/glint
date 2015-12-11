package glint.models.client

import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

/**
  * A big matrix supporting basic parameter server row-wise and element-wise operations
  *
  * @tparam V The type of values to store
  */
trait BigVector[V] extends Serializable {

  /**
    * Pulls a set of elements
    *
    * @param keys The indices of the elements
    * @return A future containing the values of the elements at given rows, columns
    */
  def pull(keys: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]]

  /**
    * Pushes a set of values
    *
    * @param keys The indices of the rows
    * @param values The values to update
    * @return A future containing either the success or failure of the operation
    */
  def push(keys: Array[Long], values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean]

}
