package glint.models

import akka.actor.{ActorRef, Actor}
import com.typesafe.scalalogging.StrictLogging
import glint.messages.{Pull, Push, Response}

import scala.reflect.ClassTag

/**
 * An actor representing a partial model
 */
abstract class PartialModel[K, V : ClassTag, D] extends Actor with StrictLogging {

  override def receive: Receive = {

    case p:Pull[K] =>
      logger.debug(s"Received pull from ${sender().path}")
      sender ! Response[V](p.keys.map(key => getValue(key)))

    case p:Push[K, D] =>
      logger.debug(s"Received push from ${sender().path}")
      p.keys.zip(p.values).foreach{ case (key, value) => setValue(key, value)}

    case x =>
      logger.warn(s"Received unknown message format of type ${x.getClass}")

  }

  /**
   * Gets the value for a particular key
   *
   * @param key The key
   * @return The corresponding value
   */
  protected def getValue(key: K): V

  /**
   * Sets the value for a particular key
   * @param key The key
   * @param value The value
   */
  protected def setValue(key: K, value: D): Unit

}
