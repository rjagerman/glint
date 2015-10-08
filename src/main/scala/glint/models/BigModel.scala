package glint.models

import akka.actor.{ActorRef, ActorSystem}

/**
 * A big model consisting out of smaller partial models at remote actors
 *
 * @param partition Function that converts a key of type K to a PartialModelRef, effectively mapping the key space to
 *                  the partial models
 * @tparam K The type of keys
 * @tparam V The type of values
 * @tparam D The type of push values
 */
class BigModel[K, V, D](val partition: K => PartialModelRef) {

  def pull(keys: Array[K], finish: Array[V] => Unit): Unit = {
    keys.groupBy(partition).foreach {
      case (model, keysForModel) => // send Pull request with these keys to model
    }
    // Ensure asynchronous callbacks when pull requests complete
    // When all pull requests are finished, call the "finish" function call back

  }

  def push(keys: Array[K], values: Array[D], finish: () => Unit): Unit = {
    keys.zip(values).groupBy{ case (k,v) => partition(k) }.foreach {
      case (model, list) => // ... send Push request with list of (K,D) key/value pairs to model
    }
  }

}
