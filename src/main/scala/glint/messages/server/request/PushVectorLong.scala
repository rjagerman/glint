package glint.messages.server.request

/**
  * A push request for vectors containing longs
  *
  * @param keys The indices
  * @param values The values to add
  */
private[glint] case class PushVectorLong(keys: Array[Long], values: Array[Long]) extends Request
