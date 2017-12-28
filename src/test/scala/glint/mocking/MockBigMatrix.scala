package glint.mocking

import akka.pattern.AskTimeoutException
import akka.util.Timeout
import breeze.linalg.{DenseVector, Vector}
import glint.models.client.BigMatrix

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A mock big matrix that stores all data internally
  *
  * @param nrOfRows  The number of rows
  * @param cols      The number of cols
  * @param default   The default value
  * @param aggregate Aggregation function for combining two values (typically addition)
  * @tparam V The type of values to store
  */
class MockBigMatrix[V: ClassTag](nrOfRows: Int, val cols: Int, default: V,
                                 aggregate: (V, V) => V) extends BigMatrix[V] {

  val rows: Long = nrOfRows

  private val data = Array.fill[Array[V]](nrOfRows)(Array.fill[V](cols)(default))
  private var destroyed: Boolean = false

  /**
    * Set this to a number to intentionally fail the next n pulls
    */
  var failNextPulls: Int = 0

  /**
    * Set this to a number to intentionally fail the next n pushes
    */
  var failNextPushes: Int = 0

  private def failNextPull(): Boolean = failNextPulls > 0

  private def failNextPush(): Boolean = failNextPushes > 0

  /**
    * Pulls a set of rows
    *
    * @param rows    The indices of the rows
    * @param timeout The timeout for this request
    * @param ec      The implicit execution context in which to execute the request
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit ec: ExecutionContext): Future[Array[Vector[V]]] = {
    if (failNextPull || destroyed) {
      failNextPulls -= 1
      fail()
    } else {
      Future {
        val array = new Array[Vector[V]](rows.length)
        var i = 0
        while (i < rows.length) {
          array(i) = DenseVector(data(rows(i).toInt))
          i += 1
        }
        array
      }
    }
  }

  /**
    * Destroys the big matrix and its resources on the parameter server
    *
    * @param timeout The timeout for this request
    * @param ec      The implicit execution context in which to execute the request
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit ec: ExecutionContext): Future[Boolean] = {
    destroyed = true
    Future {
      true
    }
  }

  /**
    * Pushes a set of values
    *
    * @param rows    The indices of the rows
    * @param cols    The indices of the columns
    * @param values  The values to update
    * @param timeout The timeout for this request
    * @param ec      The implicit execution context in which to execute the request
    * @return A future containing either the success or failure of the operation
    */
  override def push(rows: Array[Long],
                    cols: Array[Int],
                    values: Array[V])(implicit ec: ExecutionContext): Future[Boolean] = {
    if (failNextPush || destroyed) {
      failNextPushes -= 1
      fail()
    } else {
      Future {
        var i = 0
        while (i < rows.length) {
          val row = rows(i).toInt
          val col = cols(i)
          data(row)(col) = aggregate(data(row)(col), values(i))
          i += 1
        }
        true
      }
    }
  }

  /**
    * Pulls a set of elements
    *
    * @param rows    The indices of the rows
    * @param cols    The corresponding indices of the columns
    * @param timeout The timeout for this request
    * @param ec      The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long],
                    cols: Array[Int])(implicit ec: ExecutionContext): Future[Array[V]] = {
    if (failNextPull || destroyed) {
      failNextPulls -= 1
      fail()
    } else {
      Future {
        val array = new Array[V](rows.length)
        var i = 0
        while (i < rows.length) {
          array(i) = data(rows(i).toInt)(cols(i))
          i += 1
        }
        array
      }
    }
  }

  /**
    * Save the big matrix to HDFS specific path with username
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the vector was successfully destroyed
    */
  override def save(path: String, user: String)(implicit ec: ExecutionContext): Future[Boolean] = ???


  /**
    * Intentionally fails a future
    *
    * @tparam T The return type of the future
    * @return A future that intentionally fails with an AskTimeoutException
    */
  private def fail[T]()(implicit ec: ExecutionContext): Future[T] = {
    Future {
      throw new AskTimeoutException("Intentional mock failure")
    }
  }
}
