package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixFloat}
import glint.messages.server.response.ResponseFloat

/**
  * A partial matrix holding floats
  *
  * @param start The row start index
  * @param end The row end index
  * @param cols The number of columns
  */
private[glint] class PartialMatrixFloat(start: Long,
                                       end: Long,
                                       cols: Int) extends PartialMatrix[Float](start, end, cols) {

  override val data: Matrix[Float] = DenseMatrix.zeros[Float](rows, cols)

  @inline
  override def aggregate(value1: Float, value2: Float): Float = value1 + value2

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseFloat(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseFloat(getRows(pull.rows))
    case push: PushMatrixFloat => sender ! update(push.rows, push.cols, push.values)
  }

}
