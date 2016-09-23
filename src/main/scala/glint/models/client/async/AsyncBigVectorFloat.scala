package glint.models.client.async

import akka.actor.ActorRef
import com.typesafe.config.Config
import glint.messages.server.request.PushVectorFloat
import glint.messages.server.response.ResponseFloat
import glint.partitioning.Partitioner

/**
  * Asynchronous implementation of a BigVector for floats
  */
private[glint] class AsyncBigVectorFloat(partitioner: Partitioner,
                                         models: Array[ActorRef],
                                         config: Config,
                                         keys: Long)
  extends AsyncBigVector[Float, ResponseFloat, PushVectorFloat](partitioner, models, config, keys) {

  /**
    * Creates a push message from given sequence of keys and values
    *
    * @param id The identifier
    * @param keys The keys
    * @param values The values
    * @return A PushVectorFloat message for type V
    */
  @inline
  override protected def toPushMessage(id: Int, keys: Array[Long], values: Array[Float]): PushVectorFloat = {
    PushVectorFloat(id, keys, values)
  }

  /**
    * Extracts a value from a given response at given index
    *
    * @param response The response
    * @param index The index
    * @return The value
    */
  @inline
  override protected def toValue(response: ResponseFloat, index: Int): Float = response.values(index)

}
