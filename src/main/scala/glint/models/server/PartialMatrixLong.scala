package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixLong}
import glint.messages.server.response.ResponseLong

/**
  * A partial matrix holding longs
  *
  * @param start The row start index
  * @param end The row end index
  * @param cols The number of columns
  */
private[glint] class PartialMatrixLong(start: Long,
                                      end: Long,
                                      cols: Int) extends PartialMatrix[Long](start, end, cols) {

  override val data: Matrix[Long] = DenseMatrix.zeros[Long](rows, cols)

  @inline
  override def aggregate(value1: Long, value2: Long): Long = value1 + value2

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseLong(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseLong(getRows(pull.rows))
    case push: PushMatrixLong => sender ! update(push.rows, push.cols, push.values)
  }
}
