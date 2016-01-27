package glint.messages.server.request

/**
  * A pull request for matrix rows
  *
  * @param rows The row indices
  */
private[glint] case class PullMatrixRows(rows: Array[Long]) extends Request
