package glint.messages.server.response

/**
  * A response containing doubles
  *
  * @param values The response values
  */
private[glint] case class ResponseSparseDouble(indices: Array[Int], values: Array[Double]) extends Response
