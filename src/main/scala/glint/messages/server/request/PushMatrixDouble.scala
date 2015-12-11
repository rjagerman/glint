package glint.messages.server.request

/**
  * A push request for matrices containing doubles
  */
case class PushMatrixDouble(rows: Array[Long], cols: Array[Int], values: Array[Double]) extends Request
