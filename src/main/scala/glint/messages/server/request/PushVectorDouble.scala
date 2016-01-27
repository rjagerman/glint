package glint.messages.server.request

/**
  * A push request for vectors containing doubles
  *
  * @param keys The indices
  * @param values The values to add
  */
private[glint] case class PushVectorDouble(keys: Array[Long], values: Array[Double]) extends Request
