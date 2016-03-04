package glint.models.client.async

import akka.actor.ActorSystem
import com.typesafe.config.Config

/**
  * Helper for deserialization of matrices and vectors
  */
object DeserializationHelper {

  // The actor system
  private var as: ActorSystem = null

  /**
    * Gets an active actor system or creates one with given config if it does not yet exist
    *
    * @param config The configuration
    * @return The actor system
    */
  def getActorSystem(config: Config): ActorSystem = {
    if (as == null) {
      as = ActorSystem("GlintClient", config)
    }
    as
  }

}
