package glint.messages

/**
 * The response to a pull request
 *
 * @param values The values
 * @tparam V The value type
 */
case class Response[V](values: Array[V])
