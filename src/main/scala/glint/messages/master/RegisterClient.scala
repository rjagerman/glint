package glint.messages.master

import akka.actor.ActorRef

/**
  * Registers a client
  */
case class RegisterClient(client: ActorRef)
