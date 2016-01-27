package glint.messages.server.response

/**
  * A response containing floats
  *
  * @param values The response values
  */
private[glint] case class ResponseFloat(values: Array[Float]) extends Response
