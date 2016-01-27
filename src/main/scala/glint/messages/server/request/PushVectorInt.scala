package glint.messages.server.request

/**
  * A push request for vectors containing integers
  *
  * @param keys The indices
  * @param values The values to add
  */
private[glint] case class PushVectorInt(keys: Array[Long], values: Array[Int]) extends Request
