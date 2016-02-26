package glint.mocking

import akka.pattern.AskTimeoutException
import akka.util.Timeout
import glint.models.client.BigVector

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A mock big vector that stores all data internally
  *
  * @param keys The number of keys
  * @param default The default value
  * @param aggregate Aggregation function for combining two values (typically addition)
  * @tparam V The type of values to store
  */
class MockBigVector[V: ClassTag](keys: Int, cols: Int, default: V, aggregate: (V, V) => V) extends BigVector[V] {

  private val data = Array.fill[V](keys)(default)
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
    * Destroys the big Vector and its resources on the parameter server
    *
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the Vector was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    destroyed = true
    Future { true }
  }

  /**
    * Pushes a set of values
    *
    * @param keys The indices of the keys
    * @param values The values to update
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing either the success or failure of the operation
    */
  override def push(keys: Array[Long],
                    values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    if (failNextPush || destroyed) {
      failNextPushes -= 1
      fail()
    } else {
      Future {
        var i = 0
        while (i < keys.length) {
          data(keys(i).toInt) = aggregate(data(keys(i).toInt), values(i))
          i += 1
        }
        true
      }
    }
  }

  /**
    * Pulls a set of elements
    *
    * @param keys The indices of the keys
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(keys: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]] = {
    if (failNextPull || destroyed) {
      failNextPulls -= 1
      fail()
    } else {
      Future {
        val array = new Array[V](keys.length)
        var i = 0
        while (i < keys.length) {
          array(i) = data(keys(i).toInt)
          i += 1
        }
        array
      }
    }
  }

  /**
    * Intentionally fails a future
    * @tparam T The return type of the future
    * @return A future that intentionally fails with an AskTimeoutException
    */
  private def fail[T]()(implicit ec: ExecutionContext): Future[T] = {
    Future {
      throw new AskTimeoutException("Intentional mock failure")
    }
  }
}
