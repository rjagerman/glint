package glint.messages.server.request

/**
  * A push request for vectors containing longs
  */
case class PushVectorLong(keys: Array[Long], values: Array[Long]) extends Request
