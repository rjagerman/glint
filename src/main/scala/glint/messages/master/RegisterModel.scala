package glint.messages.master

import akka.actor.ActorRef
import glint.models.client.BigModel

/**
  * Registers a model with the master
  */
case class RegisterModel[K, V](name: String, model: BigModel[K, V], client: ActorRef)
