package glint.messages.server.response

/**
  * A response containing longs
  *
  * @param values The response values
  */
private[glint] case class ResponseRowsLong(values: Array[Array[Long]], columns: Int) extends Response
