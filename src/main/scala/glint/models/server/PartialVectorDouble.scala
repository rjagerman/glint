package glint.models.server

import glint.messages.server.request.{PullVector, PushVectorDouble, Save}
import glint.messages.server.response.ResponseDouble
import glint.partitioning.Partition
import spire.implicits._

/**
  * A partial vector holding doubles
  *
  * @param partition The partition
  */
private[glint] class PartialVectorDouble(partition: Partition) extends PartialVector[Double](partition) {

  override val data: Array[Double] = new Array[Double](size)

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseDouble(get(pull.keys))
    case push: PushVectorDouble =>
      update(push.keys, push.values)
      updateFinished(push.id)
    case save: Save => sender ! write(save.path, save.user)
    case x => handleLogic(x, sender)
  }

}
