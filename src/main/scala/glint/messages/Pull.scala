package glint.messages

/**
 * A basic pull message requesting the values of given list of keys
 *
 * @param keys The requested keys
 */
case class Pull(keys: Array[Long])
