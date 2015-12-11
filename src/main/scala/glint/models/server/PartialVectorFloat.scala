package glint.models.server

import breeze.linalg.{DenseVector, Vector}
import glint.messages.server.request.{PullVector, PushVectorFloat}
import glint.messages.server.response.ResponseFloat

/**
  * A partial vector holding floats
  */
class PartialVectorFloat(start: Long, end: Long) extends PartialVector[Float](start, end) {

  override val data: Vector[Float] = DenseVector.zeros[Float](size)

  @inline
  override def aggregate(value1: Float, value2: Float): Float = value1 + value2

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseFloat(get(pull.keys))
    case push: PushVectorFloat => sender ! update(push.keys, push.values)
  }

}
