package glint.messages.master

import akka.actor.ActorRef

/**
  * Message that registers a parameter server
  *
  * @param server Reference to the server actor
  */
private[glint] case class RegisterServer(val server: ActorRef)
