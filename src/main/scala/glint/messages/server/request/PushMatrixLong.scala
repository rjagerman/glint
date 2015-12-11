package glint.messages.server.request

/**
  * A push request for matrices containing longs
  */
case class PushMatrixLong(rows: Array[Long], cols: Array[Int], values: Array[Long]) extends Request
