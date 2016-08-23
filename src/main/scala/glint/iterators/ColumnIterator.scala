package glint.iterators

import akka.util.Timeout
import glint.models.client.BigMatrix

import scala.concurrent.{ExecutionContext, Future}

/**
  * An iterator over the columns of a matrix (much more overhead than row iterator due to the way the matrix is stored)
  * Attempts to prefetch next columns through a pipelined design
  *
  * @param matrix The matrix
  * @param ec The implicit execution context in which to execute requests
  * @tparam V The type of values
  */
class ColumnIterator[V](val matrix: BigMatrix[V])(implicit ec: ExecutionContext)
  extends PipelineIterator[Array[V]] {

  total = if (matrix.cols == 0 || matrix.rows == 0) {
    0
  } else {
    matrix.cols
  }

  override protected def fetchNextFuture(): Future[Array[V]] = {
    val rows = (0L until matrix.rows).toArray
    val cols = Array.fill[Int](matrix.rows.toInt)(index)
    matrix.pull(rows, cols)
  }

}
