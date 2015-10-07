package glint.messages

/**
 * A basic push message providing some values for a list of keys
 *
 * @param keys The keys
 * @param values The values
 */
case class Push[P](keys: Array[Long], values: Array[P])
