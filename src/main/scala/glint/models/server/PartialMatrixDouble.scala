package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixDouble, Save}
import glint.messages.server.response.{ResponseDouble, ResponseRowsDouble}
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial matrix holding doubles
  *
  * @param partition The partition
  * @param cols The number of columns
  */
private[glint] class PartialMatrixDouble(partition: Partition,
                                         cols: Int) extends PartialMatrix[Double](partition, cols) {

  //DenseMatrix.zeros[Double](rows, cols)
  override val data: Array[Array[Double]] = Array.fill(rows)(Array.fill[Double](cols)(0.0))

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseDouble(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseRowsDouble(getRows(pull.rows), cols)
    case push: PushMatrixDouble =>
      update(push.rows, push.cols, push.values)
      updateFinished(push.id)
    case Save(path, conf) => sender ! save(path, conf)
    case x => handleLogic(x, sender)
  }

}
