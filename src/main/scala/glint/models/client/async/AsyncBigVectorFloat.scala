package glint.models.client.async

import akka.actor.ActorRef
import com.typesafe.config.Config
import glint.indexing.Indexer
import glint.messages.server.request.PushVectorFloat
import glint.messages.server.response.ResponseFloat
import glint.partitioning.Partitioner

/**
  * Asynchronous implementation of a BigVector for floats
  */
class AsyncBigVectorFloat(partitioner: Partitioner[ActorRef],
                          indexer: Indexer[Long],
                          config: Config,
                          keys: Long)
  extends AsyncBigVector[Float, ResponseFloat, PushVectorFloat](partitioner, indexer, config, keys) {

  /**
    * Creates a push message from given sequence of keys and values
    *
    * @param keys The keys
    * @param values The values
    * @return A PushVectorFloat message for type V
    */
  override protected def toPushMessage(keys: Array[Long], values: Array[Float]): PushVectorFloat = {
    PushVectorFloat(keys, values)
  }

  /**
    * Extracts a value from a given response at given index
    *
    * @param response The response
    * @param index The index
    * @return The value
    */
  override protected def toValue(response: ResponseFloat, index: Int): Float = response.values(index)

}
