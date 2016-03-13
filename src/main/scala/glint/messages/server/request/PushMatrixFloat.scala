package glint.messages.server.request

/**
  * A push request for matrices containing floats
  *
  * @param rows The row indices
  * @param cols The column indices
  * @param values The values to add
  */
private[glint] case class PushMatrixFloat(id: Int, rows: Array[Long], cols: Array[Int], values: Array[Float]) extends Request
