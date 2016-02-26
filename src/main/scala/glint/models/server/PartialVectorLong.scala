package glint.models.server

import glint.messages.server.request.{PullVector, PushVectorLong}
import glint.messages.server.response.ResponseLong
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial vector holding longs
  *
  * @param partition The partition
  */
private[glint] class PartialVectorLong(partition: Partition) extends PartialVector[Long](partition) {

  override val data: Array[Long] = new Array[Long](size)

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseLong(get(pull.keys))
    case push: PushVectorLong => sender ! update(push.keys, push.values)
  }

}
