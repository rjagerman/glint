package glint.models.client.buffered

import java.util.concurrent.Semaphore

import akka.util.Timeout
import breeze.linalg.Vector
import glint.models.client.BigMatrix

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.ClassTag

/**
  * A buffered big matrix that buffers numerous pushes before sending them in one go.
  *
  * {{{
  *   bufferedBigMatrix = new BufferedBigMatrix[Double](someBigMatrix, 1000)
  *   bufferedBigMatrix.bufferedPush(2, 6, 10.0)
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
  private val writeLock = new Semaphore(1)
  private var bufferIndex: Int = 0
  private var promise: Promise[Boolean] = Promise[Boolean]()

  /**
    * Pulls a set of rows
    *
    * @param rows The indices of the rows
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[Vector[V]]] = {
    underlying.pull(rows)
  }

  /**
    * Destroys the big matrix and its resources on the parameter server
    *
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = underlying.destroy()

  /**
    * Pushes a set of values
    *
    * @param rows The indices of the rows
    * @param cols The indices of the columns
    * @param values The values to update
    * @return A future containing either the success or failure of the operation
    */
  override def push(rows: Array[Long],
                    cols: Array[Int],
                    values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    underlying.push(rows, cols, values)
  }

  /**
    * Pushes a value into the buffer.
    *
    * @param row The row
    * @param col The column
    * @param value The value
    */
  def bufferedPush(row: Long, col: Int, value: V)(implicit timeout: Timeout, ec: ExecutionContext): Unit = {
    bufferRows(bufferIndex) = row
    bufferCols(bufferIndex) = col
    bufferValues(bufferIndex) = value
    bufferIndex += 1
  }

  /**
    * Flushes the buffer to the parameter server.
    *
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
    * @return True if the buffer is full, false otherwise
    */
  def isFull: Boolean = size == bufferSize

  /**
    * @return The size of the current buffer (in number of elements)
    */
  def size: Int = bufferIndex

  /**
    * Pulls a set of elements
    *
    * @param rows The indices of the rows
    * @param cols The corresponding indices of the columns
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long],
                    cols: Array[Int])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]] = {
    underlying.pull(rows, cols)
  }
}
