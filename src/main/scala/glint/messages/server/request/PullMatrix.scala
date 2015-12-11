package glint.messages.server.request

/**
  * A pull request for matrices
  */
case class PullMatrix(rows: Array[Long], cols: Array[Int]) extends Request
