package glint.messages.server.request

/**
  * A pull request for vectors
  */
case class PullVector(keys: Array[Long]) extends Request
