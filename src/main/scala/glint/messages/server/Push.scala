package glint.messages.server

/**
  * A basic push message providing some values for a list of keys
  *
  * @param keys The keys
  * @param values The values
  * @tparam K The key type
  * @tparam V The value type
  */
case class Push[K, V](keys: Array[K], values: Array[V])
