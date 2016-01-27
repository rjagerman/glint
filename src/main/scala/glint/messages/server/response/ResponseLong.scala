package glint.messages.server.response

/**
  * A response containing longs
  *
  * @param values The response values
  */
private[glint] case class ResponseLong(values: Array[Long]) extends Response
