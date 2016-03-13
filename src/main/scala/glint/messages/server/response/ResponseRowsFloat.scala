package glint.messages.server.response

/**
  * A response containing floats
  *
  * @param values The response values
  */
private[glint] case class ResponseRowsFloat(values: Array[Array[Float]], columns: Int) extends Response
