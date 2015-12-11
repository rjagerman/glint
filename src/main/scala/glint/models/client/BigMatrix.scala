package glint.models.client

import akka.util.Timeout
import breeze.linalg.Vector

import scala.concurrent.{ExecutionContext, Future}

/**
  * A big matrix supporting basic parameter server row-wise and element-wise operations
  *
  * @tparam V The type of values to store
  */
trait BigMatrix[V] extends Serializable {

  /**
    * Pulls a set of rows
    *
    * @param rows The indices of the rows
    * @return A future containing the vectors representing the rows
    */
  def pull(rows: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[Vector[V]]]

  /**
    * Pulls a set of elements
    *
    * @param rows The indices of the rows
    * @param cols The corresponding indices of the columns
    * @return A future containing the values of the elements at given rows, columns
    */
  def pull(rows: Array[Long], cols: Array[Int])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]]

  /**
    * Pushes a set of values
    *
    * @param rows The indices of the rows
    * @param cols The indices of the columns
    * @param values The values to update
    * @return A future containing either the success or failure of the operation
    */
  def push(rows: Array[Long], cols: Array[Int], values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean]

}
