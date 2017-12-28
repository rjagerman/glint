package glint.models.client.granular

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import glint.models.client.BigVector

/**
  * A [[glint.models.client.BigVector BigVector]] whose messages are limited to a specific maximum message size. This
  * helps resolve timeout exceptions and heartbeat failures in akka at the cost of additional message overhead.
  *
  * {{{
  *   vector = client.vector[Double](1000000)
  *   granularVector = new GranularBigVector[Double](vector, 1000)
  *   granularVector.pull(veryLargeArrayOfIndices)
  * }}}
  *
  * @param underlying The underlying big vector
  * @param maximumMessageSize The maximum message size
  * @tparam V The type of values to store
  */
class GranularBigVector[V: ClassTag](underlying: BigVector[V],
                                     maximumMessageSize: Int) extends BigVector[V] {

  require(maximumMessageSize > 0, "Max message size must be non-zero")

  val size = underlying.size

  /**
    * Pulls a set of values while attempting to keep individual network messages smaller
    * than `maximumMessageSize`
     *
     * @param keys The indices of the elements
     * @param ec The implicit execution context in which to execute the request
     * @return A future containing the values of the elements at given rows, columns
     */
  override def pull(keys: Array[Long])(implicit ec: ExecutionContext): Future[Array[V]] = {
    var i = 0
    var current = 0
    val maxIncrement = Math.max(1, maximumMessageSize)
    val a = new Array[Future[Array[V]]](Math.ceil(keys.length.toDouble / maxIncrement.toDouble).toInt)
    while (i < keys.length) {
      val end = Math.min(keys.length, i + maxIncrement)
      val future = underlying.pull(keys.slice(i, end))
      a(current) = future
      current += 1
      i += maxIncrement
    }
    Future.sequence(a.toIterator).map {
      case arrayOfValues =>
        val finalValues = new ArrayBuffer[V](keys.length)
        arrayOfValues.foreach(x => finalValues.appendAll(x))
        finalValues.toArray
    }
  }

  override def destroy()(implicit ec: ExecutionContext): Future[Boolean] = underlying.destroy()

  /**
    * Pushes a set of values while keeping individual network messages smaller
    * than `maximumMessageSize`
     *
     * @param keys The indices of the rows
     * @param values The values to update
     * @param ec The implicit execution context in which to execute the request
     * @return A future containing either the success or failure of the operation
     */
  override def push(keys: Array[Long], values: Array[V])
    (implicit ec: ExecutionContext): Future[Boolean] = {
    var i = 0
    val ab = new ArrayBuffer[Future[Boolean]](keys.length / maximumMessageSize)
    while (i < keys.length) {
      val end = Math.min(keys.length, i + maximumMessageSize)
      val future = underlying.push(keys.slice(i, end), values.slice(i, end))
      ab.append(future)
      i += maximumMessageSize
    }
    Future.sequence(ab.toIterator).transform(x => x.forall(y => y), err => err)
  }

  /**
    * Save the big vector to HDFS specific path with username
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the vector was successfully destroyed
    */
  override def save(path: String, user: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    underlying.save(path, user)
  }

  }
