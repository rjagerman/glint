package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixInt}
import glint.messages.server.response.{ResponseRowsInt, ResponseInt}
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial matrix holding integers
  *
  * @param partition The partition
  * @param cols The number of columns
  */
private[glint] class PartialMatrixInt(partition: Partition,
                                      cols: Int) extends PartialMatrix[Int](partition, cols) {

  //override val data: Matrix[Int] = DenseMatrix.zeros[Int](rows, cols)
  override val data: Array[Array[Int]] = Array.fill(rows)(Array.fill[Int](cols)(0))

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseInt(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseRowsInt(getRows(pull.rows), cols)
    case push: PushMatrixInt => sender ! update(push.rows, push.cols, push.values)
  }

}
