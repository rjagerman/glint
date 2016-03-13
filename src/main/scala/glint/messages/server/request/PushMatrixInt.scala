package glint.messages.server.request

/**
  * A push request for matrices containing integers
  *
  * @param rows The row indices
  * @param cols The column indices
  * @param values The values to add
  */
private[glint] case class PushMatrixInt(id: Int, rows: Array[Long], cols: Array[Int], values: Array[Int]) extends Request
