package glint.messages.master

import akka.actor.ActorRef

/**
  * Registers a model with the master
  */
case class RegisterModel[K, V](name: String, client: ActorRef)
