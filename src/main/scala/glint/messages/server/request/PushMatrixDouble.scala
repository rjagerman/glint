package glint.messages.server.request

/**
  * A push request for matrices containing doubles
  *
  * @param id The push identification
  * @param rows The row indices
  * @param cols The column indices
  * @param values The values to add
  */
private[glint] case class PushMatrixDouble(id: Int, rows: Array[Long], cols: Array[Int], values: Array[Double]) extends Request
