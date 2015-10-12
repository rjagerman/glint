package glint.models.array

import akka.actor.{ActorLogging, Actor}
import akka.actor.Actor.Receive
import com.typesafe.scalalogging.StrictLogging
import glint.messages.server.{Response, Push, Pull}

import scala.reflect.ClassTag

/**
 * A partial model with an underlying data representation of an array
 *
 * @param start The start index
 * @param end The end index
 * @param default The default value
 * @tparam V The type of values to store
 */
class ArrayPartialModel[V : ClassTag](val start: Long,
                                      val end: Long,
                                      val default: V) extends Actor with ActorLogging {

  val data: Array[V] = Array.fill[V]((end - start).toInt)(default)

  override def receive: Receive = {
    case p: Pull[Long] =>
      log.info(s"Received pull request from ${sender.path.address}")
      sender ! new Response[V](p.keys.map(k => data(index(k))).toArray)

    case p: Push[Long, V] =>
      log.info(s"Received push request from ${sender.path.address}")
      p.keys.zip(p.values).foreach {
        case (k, v) => data(index(k)) = v
      }
  }

  def index(key: Long): Int = {
    assert(key >= start)
    assert(key < end)
    (key - start).toInt
  }

}
