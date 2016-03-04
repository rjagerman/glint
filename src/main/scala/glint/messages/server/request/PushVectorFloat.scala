package glint.messages.server.request

/**
  * A push request for vectors containing floats
  *
  * @param keys The indices
  * @param values The values to add
  */
private[glint] case class PushVectorFloat(id: Int, keys: Array[Long], values: Array[Float]) extends Request
