package glint.messages.master

import akka.actor.ActorRef

/**
 * Message send by a parameter server to the manager when it attempts to register itself
 */
case class RegisterServer(val server: ActorRef)
