package glint.models.client.buffered

import akka.util.Timeout
import breeze.linalg.Vector
import glint.models.client.BigMatrix

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A [[glint.models.client.BigMatrix BigMatrix]] that buffers numerous pushes before sending them in one go
  *
  * {{{
  *   matrix = client.matrix[Double](10000, 100)
  *   bufferedMatrix = new BufferedBigMatrix[Double](matrix, 1000)
  *   bufferedMatrix.bufferedPush(2, 6, 10.0)
  *   if (bufferedBigMatrix.isFull) {
  *     bufferedBigMatrix.flush()
  *   }
  * }}}
  *
  * @param underlying The underlying big matrix implementation
  * @param bufferSize The buffer size
  * @tparam V The type of values to store
  */
class BufferedBigMatrix[@specialized V: ClassTag](underlying: BigMatrix[V], bufferSize: Int) extends BigMatrix[V] {

  private val bufferRows = new Array[Long](bufferSize)
  private val bufferCols = new Array[Int](bufferSize)
  private val bufferValues = new Array[V](bufferSize)
  private var bufferIndex: Int = 0

  /**
    * Pulls a set of rows using the underlying BigMatrix implementation
    *
    * @param rows The indices of the rows
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[Vector[V]]] = {
    underlying.pull(rows)
  }

  /**
    * Destroys the underlying big matrix and its resources on the parameter server
    *
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = underlying.destroy()

  /**
    * Pushes a set of values using the underlying BigMatrix implementation
    *
    * @param rows The indices of the rows
    * @param cols The indices of the columns
    * @param values The values to update
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing either the success or failure of the operation
    */
  override def push(rows: Array[Long],
                    cols: Array[Int],
                    values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    underlying.push(rows, cols, values)
  }

  /**
    * Pushes a value into the buffer as long as there is space. If the buffer is full this method will automatically
    * flush the buffer to the parameter server. It will either return an option that is either set to a future when the
    * buffer was flushed or set to None when the buffer is not yet full.
    *
    * @param row The row
    * @param col The column
    * @param value The value
    * @return An option set to Future[Boolean] if a flush occurred or None if no flush occurred
    */
  def bufferedPush(row: Long,
                   col: Int,
                   value: V)(implicit timeout: Timeout, ec: ExecutionContext): Option[Future[Boolean]] = {
    bufferRows(bufferIndex) = row
    bufferCols(bufferIndex) = col
    bufferValues(bufferIndex) = value
    bufferIndex += 1
    flushIfFull()
  }

  /**
    * Flushes the buffer to the parameter server.
    *
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the success or failure of the operation
    */
  def flush()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    if (bufferIndex == 0) {
      Future {
        true
      }
    } else {
      var index: Int = 0
      val pushRows = new Array[Long](bufferIndex)
      val pushCols = new Array[Int](bufferIndex)
      val pushValues = new Array[V](bufferIndex)
      while (index < bufferIndex) {
        pushRows(index) = bufferRows(index)
        pushCols(index) = bufferCols(index)
        pushValues(index) = bufferValues(index)
        index += 1
      }
      bufferIndex = 0
      underlying.push(pushRows, pushCols, pushValues)
    }
  }

  /**
    * Flushes the buffer to the parameter server if it is full.
    *
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the success or failure of the operation
    */
  def flushIfFull()(implicit timeout: Timeout, ec: ExecutionContext): Option[Future[Boolean]] = {
    if (isFull) {
      Some(flush())
    } else {
      None
    }
  }

  /**
    * @return True if the buffer is full, false otherwise
    */
  def isFull: Boolean = size == bufferSize

  /**
    * @return The size of the current buffer (in number of elements)
    */
  def size: Int = bufferIndex

  /**
    * Pulls a set of elements using the underlying BigMatrix implementation
    *
    * @param rows The indices of the rows
    * @param cols The corresponding indices of the columns
    * @param timeout The timeout for this request
    * @param ec The implicit execution context in which to execute the request
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long],
                    cols: Array[Int])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]] = {
    underlying.pull(rows, cols)
  }
}
