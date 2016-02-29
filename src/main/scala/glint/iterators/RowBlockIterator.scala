package glint.iterators

import akka.util.Timeout
import breeze.linalg.Vector
import glint.models.client.BigMatrix

import scala.concurrent.{Await, Future, ExecutionContext}

/**
  * This is an iterator over blocks of rows of a big matrix that attempts to prefetch next blocks through a pipelined
  * design
  *
  * @param matrix The big matrix to iterate over
  * @param blockSize How many rows to retrieve per iteration
  * @param ec The implicit execution context in which to execute requests
  * @param timeout The timeout on requests before they are considered lost
  */
class RowBlockIterator[V](val matrix: BigMatrix[V],
                          val blockSize: Int)(implicit ec: ExecutionContext, timeout: Timeout)
  extends PipelineIterator[Array[Vector[V]]] {

  if (matrix.cols == 0 || matrix.rows == 0) {
    total = 0
  } else {
    val inc = if (matrix.rows % blockSize == 0) { 0 } else { 1 }
    total = inc + (matrix.rows / blockSize).toInt
  }

  override protected def fetchNextFuture(): Future[Array[Vector[V]]] = {
    val nextRows = (index.toLong * blockSize until Math.min(matrix.rows, (index + 1) * blockSize)).toArray
    matrix.pull(nextRows)
  }

}
