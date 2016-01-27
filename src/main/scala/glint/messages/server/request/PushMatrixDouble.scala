package glint.messages.server.request

/**
  * A push request for matrices containing doubles
  *
  * @param rows The row indices
  * @param cols The column indices
  * @param values The values to add
  */
private[glint] case class PushMatrixDouble(rows: Array[Long], cols: Array[Int], values: Array[Double]) extends Request
