package glint.messages.server.response

/**
  * A response containing integers
  *
  * @param values The response values
  */
private[glint] case class ResponseRowsInt(values: Array[Array[Int]], columns: Int) extends Response
