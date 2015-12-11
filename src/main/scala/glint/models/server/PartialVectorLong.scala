package glint.models.server

import breeze.linalg.{DenseVector, Vector}
import glint.messages.server.request.{PullVector, PushVectorLong}
import glint.messages.server.response.ResponseLong

/**
  * A partial vector holding longs
  */
class PartialVectorLong(start: Long, end: Long) extends PartialVector[Long](start, end) {

  override val data: Vector[Long] = DenseVector.zeros[Long](size)

  @inline
  override def aggregate(value1: Long, value2: Long): Long = value1 + value2

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseLong(get(pull.keys))
    case push: PushVectorLong => sender ! update(push.keys, push.values)
  }

}
