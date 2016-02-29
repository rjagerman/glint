package glint.iterators

import akka.util.Timeout
import breeze.linalg.Vector
import glint.models.client.BigMatrix

import scala.concurrent.ExecutionContext

/**
  * An iterator over the rows of a matrix
  *
  * @param matrix The underlying matrix to iterate over
  * @param blockSize The number of rows to pull per request (default: 100)
  */
class RowIterator[V](matrix: BigMatrix[V], blockSize: Int = 100)(implicit val ec: ExecutionContext, timeout: Timeout)
  extends Iterator[Vector[V]] {

  // Row progress
  var index: Long = 0
  val rows: Long = if (matrix.rows == 0 || matrix.cols == 0) {
    0L
  } else {
    matrix.rows
  }

  // The underlying block iterator
  val blockIterator = new RowBlockIterator[V](matrix, blockSize)

  // The local block progress
  var localIndex: Int = 0
  var localSize: Int = 0
  var block = new Array[Vector[V]](0)

  override def hasNext: Boolean = index < rows

  override def next(): Vector[V] = {
    if (localIndex >= localSize) {
      block = blockIterator.next()
      localIndex = 0
      localSize = block.length
    }
    localIndex += 1
    index += 1
    block(localIndex - 1)
  }

}
