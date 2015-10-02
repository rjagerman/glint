package ch.ethz.inf.da.glint

import akka.actor.Actor
import akka.actor.Actor.Receive
import ch.ethz.inf.da.glint.messages.{Push, Pull}

import scala.reflect.ClassTag

/**
 * A parameter server which stores the parameters and handles pull and push requests
 *
 * @param size The number of keys to store on this parameter server
 * @param startIndex Starting index of keys stored on this parameter server
 * @param construct A constructor that builds the initial values
 * @param push Handler for push events
 * @param pull Handler for pull events
 */
class ParamServer[V: ClassTag, P](size: Int,
                                  startIndex: Long,
                                  construct:  => V,
                                  push: P => V,
                                  pull: Long => V) extends Actor {

  /**
   * The data
   */
  val data = Array.fill[V](size)(construct)

  override def receive: Receive = {

    /**
     * Pull operation
     * This method will send communication back to the original sender with the requested information
     */
    case Pull(keys) => sender ! keys.map(pull)

    /**
     * Push operation
     * This method will obtain values and process them locally (e.g. update a weight vector)
     */
    case p: Push[P] => p.keys.zip(p.values) foreach {
      case (k, v) => data(toLocal(k)) = push(v)
    }

  }

  /**
   * Helper method converting a global key to a local one
   *
   * @param key The key
   * @return The local index for the data array
   */
  private def toLocal(key: Long): Int = (key - startIndex).toInt

}
