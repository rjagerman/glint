package glint.messages.server.response

/**
  * A response containing integers
  *
  * @param values The response values
  */
private[glint] case class ResponseInt(values: Array[Int]) extends Response
