package glint.models.client.granular

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import breeze.linalg.Vector
import glint.models.client.BigMatrix

/**
  * A [[glint.models.client.BigMatrix BigMatrix]] whose messages are limited to a specific maximum message size. This
  * helps resolve timeout exceptions and heartbeat failures in akka at the cost of additional message overhead.
  *
  * {{{
  *   matrix = client.matrix[Double](1000000, 100)
  *   granularMatrix = new GranularBigMatrix[Double](matrix, 1000)
  *   granularMatrix.pull(veryLargeArrayOfRowIndices)
  * }}}
  *
  * @param underlying The underlying big matrix
  * @param maximumMessageSize The maximum message size
  * @tparam V The type of values to store
  */
class GranularBigMatrix[V: ClassTag](underlying: BigMatrix[V],
                                     maximumMessageSize: Int) extends BigMatrix[V] {

  require(maximumMessageSize > 0, "Max message size must be non-zero")

  val rows: Long = underlying.rows
  val cols: Int = underlying.cols

  /**
    * Pulls a set of rows while attempting to keep individual network messages smaller than `maximumMessageSize`
    *
    * @param rows The indices of the rows
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit ec: ExecutionContext): Future[Array[Vector[V]]] = {
    if (rows.length * cols <= maximumMessageSize) {
      underlying.pull(rows)
    } else {
      var i = 0
      var current = 0
      val maxIncrement = Math.max(1, maximumMessageSize / cols)
      val a = new Array[Future[Array[Vector[V]]]](Math.ceil(rows.length.toDouble / maxIncrement.toDouble).toInt)
      while (i < rows.length) {
        val end = Math.min(rows.length, i + maxIncrement)
        val future = underlying.pull(rows.slice(i, end))
        a(current) = future
        current += 1
        i += maxIncrement
      }
      Future.sequence(a.toIterator).map {
        case arrayOfValues =>
          val finalValues = new ArrayBuffer[Vector[V]](rows.length)
          arrayOfValues.foreach(x => finalValues.appendAll(x))
          finalValues.toArray
      }
    }
  }

  /**
    * Destroys the big matrix and its resources on the parameter server
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit ec: ExecutionContext): Future[Boolean] = underlying.destroy()

  /**
    * Pushes a set of values while keeping individual network messages smaller than `maximumMessageSize`
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
    if (rows.length <= maximumMessageSize) {
      underlying.push(rows, cols, values)
    } else {
      var i = 0
      val ab = new ArrayBuffer[Future[Boolean]](rows.length / maximumMessageSize)
      while (i < rows.length) {
        val end = Math.min(rows.length, i + maximumMessageSize)
        val future = underlying.push(rows.slice(i, end), cols.slice(i, end), values.slice(i, end))
        ab.append(future)
        i += maximumMessageSize
      }
      Future.sequence(ab.toIterator).transform(x => x.forall(y => y), err => err)
    }
  }

  /**
    * Save the big matrix to HDFS specific path with username
    *
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the vector was successfully destroyed
    */
  override def save(path: String, user: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    underlying.save(path, user)
  }

    /**
    * Pulls a set of elements while keeping individual network messages smaller than `maximumMessageSize`
    *
    * @param rows The indices of the rows
    * @param cols The corresponding indices of the columns
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long],
                    cols: Array[Int])(implicit ec: ExecutionContext): Future[Array[V]] = {
    if (rows.length <= maximumMessageSize) {
      underlying.pull(rows, cols)
    } else {
      var i = 0
      val ab = new ArrayBuffer[Future[Array[V]]](rows.length / maximumMessageSize)
      while (i < rows.length) {
        val end = Math.min(rows.length, i + maximumMessageSize)
        val future = underlying.pull(rows.slice(i, end), cols.slice(i, end))
        ab.append(future)
        i += maximumMessageSize
      }
      Future.sequence(ab.toIterator).map {
        case arrayOfValues =>
          val finalValues = new ArrayBuffer[V](rows.length)
          arrayOfValues.foreach(x => finalValues.appendAll(x))
          finalValues.toArray
      }
    }
  }
}
