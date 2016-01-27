package glint.messages.server.response

/**
  * A response containing doubles
  *
  * @param values The response values
  */
private[glint] case class ResponseDouble(values: Array[Double]) extends Response
