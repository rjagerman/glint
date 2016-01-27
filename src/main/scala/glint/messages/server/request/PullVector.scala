package glint.messages.server.request

/**
  * A pull request for vectors
  *
  * @param keys The indices
  */
private[glint] case class PullVector(keys: Array[Long]) extends Request
