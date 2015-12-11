package glint.messages.server.request

/**
  * A push request for matrices containing floats
  */
case class PushMatrixFloat(rows: Array[Long], cols: Array[Int], values: Array[Float]) extends Request
