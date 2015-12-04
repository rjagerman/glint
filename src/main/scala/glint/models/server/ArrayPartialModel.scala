package glint.models.server

import akka.actor.{Actor, ActorLogging}
import glint.messages.server.{Pull, Push, Response}

import scala.reflect.ClassTag

/**
  * A partial model with an underlying data representation of an array
  *
  * @param start The start index
  * @param end The end index
  * @param default The default value
  * @tparam V The type of values to store
  */
abstract class ArrayPartialModel[V: ClassTag](val start: Long,
                                              val end: Long,
                                              val default: V) extends Actor with ActorLogging {

  log.debug(s"Initializing ArrayPartialModel[${implicitly[ClassTag[V]]}] of size ${end - start} (default: $default)")
  val data: Array[V] = Array.fill[V]((end - start).toInt)(default)

  override def receive: Receive = {

    // Pull request, send back the data for given keys
    case p: Pull[Long] =>
      log.info(s"Received pull request from ${sender.path.address}")
      sender ! new Response[V](p.keys.map(k => data(index(k))))

    // Push request, update local data based on given key/values using addition to update
    case p: Push[Long, V] =>
      log.info(s"Received push request from ${sender.path.address}")
      for (idx <- p.keys.indices) {
        update(p.keys(idx), p.values(idx))
      }
      sender ! true
  }

  def update(key: Long, value: V): Unit

  /**
    * Converts a global key (Long) to a local key (int) that can be used to index data
    *
    * @param key The global key
    * @return The local key
    */
  def index(key: Long): Int = {
    assert(key >= start, s"Key out of range ${key} < ${start}")
    assert(key < end, s"Key out of range ${key} >= ${end}")
    (key - start).toInt
  }

  override def postStop(): Unit = {
    log.info(s"Destroyed ArrayPartialModel[${implicitly[ClassTag[V]]}] for range [${start}, ${end})")
  }

  log.debug(s"Finished constructing ArrayPartialModel[${implicitly[ClassTag[V]]}]")

}
