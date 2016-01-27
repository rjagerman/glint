package glint.models.server

import breeze.linalg.{DenseVector, Vector}
import glint.messages.server.request.{PullVector, PushVectorInt}
import glint.messages.server.response.ResponseInt

/**
  * A partial vector holding integers
  *
  * @param start The start index
  * @param end The end index
  */
private[glint] class PartialVectorInt(start: Long, end: Long) extends PartialVector[Int](start, end) {

  override val data: Vector[Int] = DenseVector.zeros[Int](size)

  @inline
  override def aggregate(value1: Int, value2: Int): Int = value1 + value2

  override def receive: Receive = {
    case pull: PullVector => sender ! ResponseInt(get(pull.keys))
    case push: PushVectorInt => sender ! update(push.keys, push.values)
  }

}
