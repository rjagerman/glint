package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixFloat}
import glint.messages.server.response.{ResponseFloat, ResponseRowsFloat}
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial matrix holding floats
  *
  * @param partition The partition
  * @param cols The number of columns
  */
private[glint] class PartialMatrixFloat(partition: Partition,
                                       cols: Int) extends PartialMatrix[Float](partition, cols) {

  //override val data: Matrix[Float] = DenseMatrix.zeros[Float](rows, cols)
  override val data: Array[Array[Float]] = Array.fill(rows)(Array.fill[Float](cols)(0.0f))

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseFloat(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseRowsFloat(getRows(pull.rows), cols)
    case push: PushMatrixFloat => sender ! update(push.rows, push.cols, push.values)
  }

}
