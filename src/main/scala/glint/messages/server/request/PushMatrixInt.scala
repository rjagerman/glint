package glint.messages.server.request

/**
  * A push request for vectors containing doubles
  */
case class PushMatrixInt(rows: Array[Long], cols: Array[Int], values: Array[Int]) extends Request
