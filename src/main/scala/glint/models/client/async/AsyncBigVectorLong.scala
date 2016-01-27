package glint.models.client.async

import akka.actor.ActorRef
import com.typesafe.config.Config
import glint.indexing.Indexer
import glint.messages.server.request.PushVectorLong
import glint.messages.server.response.ResponseLong
import glint.partitioning.Partitioner

/**
  * Asynchronous implementation of a BigVector for longs
  */
private[glint] class AsyncBigVectorLong(partitioner: Partitioner[ActorRef],
                                        indexer: Indexer[Long],
                                        config: Config,
                                        keys: Long)
  extends AsyncBigVector[Long, ResponseLong, PushVectorLong](partitioner, indexer, config, keys) {

  /**
    * Creates a push message from given sequence of keys and values
    *
    * @param keys The keys
    * @param values The values
    * @return A PushVectorLong message for type V
    */
  @inline
  override protected def toPushMessage(keys: Array[Long], values: Array[Long]): PushVectorLong = {
    PushVectorLong(keys, values)
  }

  /**
    * Extracts a value from a given response at given index
    *
    * @param response The response
    * @param index The index
    * @return The value
    */
  @inline
  override protected def toValue(response: ResponseLong, index: Int): Long = response.values(index)

}
