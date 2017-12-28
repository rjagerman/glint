package glint.models.server

import glint.messages.server.request.{PullVector, PushVectorLong, Save}
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
    case push: PushVectorLong =>
      update(push.keys, push.values)
      updateFinished(push.id)
    case save: Save => sender ! write(save.path, save.user)
    case x => handleLogic(x, sender)
  }

}
