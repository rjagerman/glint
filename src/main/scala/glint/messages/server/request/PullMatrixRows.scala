package glint.messages.server.request

/**
  * A pull request for matrix rows
  */
case class PullMatrixRows(rows: Array[Long]) extends Request
