package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixDouble}
import glint.messages.server.response.ResponseDouble

/**
  * A partial matrix holding doubles
  *
  * @param start The row start index
  * @param end The row end index
  * @param cols The number of columns
  */
private[glint] class PartialMatrixDouble(start: Long,
                                        end: Long,
                                        cols: Int) extends PartialMatrix[Double](start, end, cols) {

  //DenseMatrix.zeros[Double](rows, cols)
  override val data: Array[Array[Double]] = Array.fill(rows)(Array.fill[Double](cols)(0.0))

  @inline
  override def aggregate(value1: Double, value2: Double): Double = value1 + value2

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseDouble(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseDouble(getRows(pull.rows))
    case push: PushMatrixDouble => sender ! update(push.rows, push.cols, push.values)
  }

}