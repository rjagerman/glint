package glint.models

import akka.actor.ActorRef

/**
 * A partial model reference
 */
class PartialModelRef(val actorRef: ActorRef, start: Long, end: Long) {

}
