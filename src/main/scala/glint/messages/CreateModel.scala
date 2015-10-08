package glint.messages

import glint.models.PartialModel

/**
 * Message to specify and create a large model
 *
 * @param size The size of the key space of the model
 * @param index A method mapping keys to (unique) long indices
 * @param construct Constructor method for values
 * @param pull Action to perform on a pull request for a single key
 * @param push Action to perform on a push request for a single key/value pair
 * @tparam K The key type
 * @tparam V The value type
 */
//case class CreateModel[K, V](size: Long, index: K => Long, construct: K => V, pull: K => V, push: (K, V) => V)


/**
 *
 * @param modelClass The class of the partial model
 * @param size
 */
case class CreateModel(modelClass : scala.Predef.Class[_], size: Long)

