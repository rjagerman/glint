package glint.messages.server.request

/**
  * A push request for vectors containing floats
  */
case class PushVectorFloat(keys: Array[Long], values: Array[Float]) extends Request
