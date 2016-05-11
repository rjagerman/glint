package glint.models.server

import breeze.linalg.{DenseMatrix, Matrix}
import glint.messages.server.request.{PullMatrix, PullMatrixRows, PushMatrixLong}
import glint.messages.server.response.{ResponseRowsLong, ResponseLong}
import glint.models.server.aggregate.Aggregate
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial matrix holding longs
  *
  * @param partition The row start index
  * @param cols The number of columns
  */
private[glint] class PartialMatrixLong(partition: Partition,
                                       cols: Int,
                                       aggregate: Aggregate) extends PartialMatrix[Long](partition, cols, aggregate) {

  //override val data: Matrix[Long] = DenseMatrix.zeros[Long](rows, cols)
  override val data: Array[Array[Long]] = Array.fill(rows)(Array.fill[Long](cols)(0L))

  override def receive: Receive = {
    case pull: PullMatrix => sender ! ResponseLong(get(pull.rows, pull.cols))
    case pull: PullMatrixRows => sender ! ResponseRowsLong(getRows(pull.rows), cols)
    case push: PushMatrixLong =>
      update(push.rows, push.cols, push.values)
      updateFinished(push.id)
    case x => handleLogic(x, sender)
  }
}
