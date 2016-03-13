package glint.models.server

import glint.messages.server.request.{PullVector, PushVectorFloat}
import glint.messages.server.response.ResponseFloat
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial vector holding floats
  *
  * @param partition The partition
  */
private[glint] class PartialVectorFloat(partition: Partition) extends PartialVector[Float](partition) {

  override val data: Array[Float] = new Array[Float](size)

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseFloat(get(pull.keys))
    case push: PushVectorFloat =>
      update(push.keys, push.values)
      updateFinished(push.id)
    case x => handleLogic(x, sender)
  }

}
