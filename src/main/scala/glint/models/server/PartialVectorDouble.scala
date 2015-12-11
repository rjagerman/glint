package glint.models.server

import breeze.linalg.{DenseVector, Vector}
import glint.messages.server.request.{PullVector, PushVectorDouble}
import glint.messages.server.response.ResponseDouble

/**
  * A partial vector holding floats
  */
class PartialVectorDouble(start: Long, end: Long) extends PartialVector[Double](start, end) {

  override val data: Vector[Double] = DenseVector.zeros[Double](size)

  @inline
  override def aggregate(value1: Double, value2: Double): Double = value1 + value2

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseDouble(get(pull.keys))
    case push: PushVectorDouble => sender ! update(push.keys, push.values)
  }

}
