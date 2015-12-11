package glint.messages.server.request

/**
  * A push request for vectors containing doubles
  */
case class PushVectorDouble(keys: Array[Long], values: Array[Double]) extends Request
