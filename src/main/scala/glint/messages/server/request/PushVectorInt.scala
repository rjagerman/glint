package glint.messages.server.request

/**
  * A push request for vectors containing integers
  */
case class PushVectorInt(keys: Array[Long], values: Array[Int]) extends Request
