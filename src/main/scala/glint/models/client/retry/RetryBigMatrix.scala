package glint.models.client.retry

import akka.util.Timeout
import breeze.linalg.Vector
import glint.models.client.BigMatrix
import retry.Success

import scala.concurrent.{Future, ExecutionContext}

/**
  * A big matrix that can retry failed requests automatically
  *
  * @param underlying The underlying big matrix
  * @param attempts The number of attempts (default: 3)
  */
class RetryBigMatrix[@specialized V](underlying: BigMatrix[V], val attempts: Int = 3) extends BigMatrix[V] {

  val rows: Long = underlying.rows
  val cols: Int = underlying.cols

  /**
    * Pulls a set of rows
    *
    * @param rows The indices of the rows
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit ec: ExecutionContext): Future[Array[Vector[V]]] = {
    implicit val success = Success.always
    retry.Directly(attempts) { () =>
      underlying.pull(rows)
    }
  }

  /**
    * Destroys the big matrix and its resources on the parameter server
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit ec: ExecutionContext): Future[Boolean] = {
    implicit val success = Success.apply[Boolean](identity)
    retry.Directly(attempts) { () =>
      underlying.destroy()
    }
  }

  /**
    * Pushes a set of values
    *
    * @param rows The indices of the rows
    * @param cols The indices of the columns
    * @param values The values to update
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing either the success or failure of the operation
    */
  override def push(rows: Array[Long],
                    cols: Array[Int],
                    values: Array[V])(implicit ec: ExecutionContext): Future[Boolean] = {
    implicit val success = Success[Boolean](identity)
    retry.Directly(attempts) { () =>
      underlying.push(rows, cols, values)
    }
  }

  /**
    * Pulls a set of elements
    *
    * @param rows The indices of the rows
    * @param cols The corresponding indices of the columns
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long],
                    cols: Array[Int])(implicit ec: ExecutionContext): Future[Array[V]] = {
    implicit val success = Success.always
    retry.Directly(attempts) { () =>
      underlying.pull(rows, cols)
    }
  }

}
