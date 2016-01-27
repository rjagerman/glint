package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixInt}
import glint.messages.server.response.ResponseInt

/**
  * A partial matrix holding integers
  *
  * @param start The row start index
  * @param end The row end index
  * @param cols The number of columns
  */
private[glint] class PartialMatrixInt(start: Long,
                                     end: Long,
                                     cols: Int) extends PartialMatrix[Int](start, end, cols) {

  override val data: Matrix[Int] = DenseMatrix.zeros[Int](rows, cols)

  @inline
  override def aggregate(value1: Int, value2: Int): Int = value1 + value2

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseInt(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseInt(getRows(pull.rows))
    case push: PushMatrixInt => sender ! update(push.rows, push.cols, push.values)
  }

}
