package glint.models.client

import akka.util.Timeout
import org.apache.hadoop.conf.Configuration

import scala.concurrent.{ExecutionContext, Future}

/**
  * A big vector supporting basic parameter server element-wise operations
  *
  * {{{
  *   val vector: BigVector[Double] = ...
  *   vector.pull(Array(0L, 100L, 30000L)) // Pull values from the vector
  *   vector.push(Array(0L, 100L, 30000L), Array(0.5, 3.14, 9.9)) // Add values to the vector
  *   vector.save(path, conf) // Save Data to HDFS
  *   vector.destroy() // Destroy vector, freeing up memory on the parameter server
  * }}}
  *
  * @tparam V The type of values to store
  */
trait BigVector[V] extends Serializable {

  val size: Long

  /**
    * Pulls a set of elements
    *
    * @param keys The indices of the elements
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  def pull(keys: Array[Long])(implicit ec: ExecutionContext): Future[Array[V]]

  /**
    * Pushes a set of values
    *
    * @param keys The indices of the rows
    * @param values The values to update
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing either the success or failure of the operation
    */
  def push(keys: Array[Long], values: Array[V])(implicit ec: ExecutionContext): Future[Boolean]

  /**
    * Destroys the big vector and its resources on the parameter server
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the vector was successfully destroyed
    */
  def destroy()(implicit ec: ExecutionContext): Future[Boolean]

  /**
    * Save the Vector to HDFS Path
    *
    * @param ec The implicit execution context in which to execute the request
    * @param path HDFS Path
    * @param conf Hadoop Cluster configuration, which will be used to store vector into HDFS
    * @return A future whether the matrix was successfully saved
    */
  def save(path: String, conf: Configuration)(implicit ec: ExecutionContext): Future[Boolean]
}
