package glint.messages.master

import akka.actor.ActorRef

/**
  * Message that registers a client
  *
  * @param client Reference to the client actor
  */
private[glint] case class RegisterClient(client: ActorRef)
