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

  val size: Long = underlying.size

  /**
    * Destroys the big Vector and its resources on the parameter server
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the Vector was successfully destroyed
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
    * @param keys The indices of the keys
    * @param values The values to update
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing either the success or failure of the operation
    */
  override def push(keys: Array[Long],
                    values: Array[V])(implicit ec: ExecutionContext): Future[Boolean] = {
    implicit val success = Success[Boolean](identity)
    retry.Directly(attempts) { () =>
      underlying.push(keys, values)
    }
  }

  /**
    * Pulls a set of elements
    *
    * @param keys The indices of the keys
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(keys: Array[Long])(implicit ec: ExecutionContext): Future[Array[V]] = {
    implicit val success = Success.always
    retry.Directly(attempts) { () =>
      underlying.pull(keys)
    }
  }

  /**
    * Save the big matrix to HDFS specific path with username
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the vector was successfully destroyed
    */
  override def save(path: String, user: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    implicit val success = Success.always
    retry.Directly(attempts) { () =>
      underlying.save(path, user)
    }
  }
}
