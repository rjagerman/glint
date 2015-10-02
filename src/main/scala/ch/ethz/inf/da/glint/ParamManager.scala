package ch.ethz.inf.da.glint

import akka.actor.Actor.Receive
import akka.actor._

/**
 * Creates a model and spawns parameter server actors accordingly
 *
 * @param keys The total number of keys
 * @param push A server-side handler for push events (e.g. update step in algorithm)
 * @param pull A server-side handler for pull events
 * @tparam P The type of the push values (e.g. gradients)
 * @tparam V The type of the stored values (e.g. weights)
 */
case class Create[P, V](keys: Long, push: (P => V), pull: (Long => V), construct: Long => V)

/**
 * The manager that handles the setup of parameter servers and most control lines
 */
class ParamManager extends Actor {

  override def receive: Receive = {
    case Create(keys, push, pull, consruct) => {

      // Todo:
      // Spawn actors on remote servers to handle the key space with given push/pull/construct functions

    }
  }

}
