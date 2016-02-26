package glint.messages.server.response

/**
  * A response containing doubles
  *
  * @param values The response values
  */
private[glint] case class ResponseRowsDouble(values: Array[Array[Double]], columns: Int) extends Response
