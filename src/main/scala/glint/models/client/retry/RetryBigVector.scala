package glint.models.client.retry

import akka.util.Timeout
import glint.models.client.BigVector
import retry.Success

import scala.concurrent.{Future, ExecutionContext}

/**
  * A big vector that can retry failed requests automatically
  *
  * @param underlying The underlying big Vector
  * @param attempts The number of attempts (default: 3)
  */
class RetryBigVector[@specialized V](underlying: BigVector[V], val attempts: Int = 3) extends BigVector[V] {

  /**
    * Destroys the big Vector and its resources on the parameter server
    *
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the Vector was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    implicit val success = Success.apply[Boolean](identity)
    retry.Directly(attempts) { () =>
      underlying.destroy()
    }
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
    implicit val success = Success[Boolean](identity)
    retry.Directly(attempts) { () =>
      underlying.push(keys, values)
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
    implicit val success = Success.always
    retry.Directly(attempts) { () =>
      underlying.pull(keys)
    }
  }

}
