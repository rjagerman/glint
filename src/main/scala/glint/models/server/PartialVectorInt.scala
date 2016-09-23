package glint.models.server

import glint.messages.server.request.{PullVector, PushVectorInt}
import glint.messages.server.response.ResponseInt
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial vector holding integers
  *
  * @param partition The partition
  */
private[glint] class PartialVectorInt(partition: Partition) extends PartialVector[Int](partition) {

  override val data: Array[Int] = new Array[Int](size)

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseInt(get(pull.keys))
    case push: PushVectorInt =>
      update(push.keys, push.values)
      updateFinished(push.id)
    case x => handleLogic(x, sender)
  }

}
