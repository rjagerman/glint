package glint

import akka.actor.{ActorSystem, Actor}
import akka.actor.Actor.Receive
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.Glint._
import glint.messages.Response

/**
 * A client interface that allows a worker node to communicate with spark workers
 */
class Client(config: Config) extends StrictLogging {

  val akkaConfig = ConfigFactory.parseString(
    s"""
      akka {
        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
        loglevel = "INFO"
        actor {
          provider = "akka.remote.RemoteActorRefProvider"
        }
        remote {
          enable-transports = ["akka.remote.netty.tcp"]
          netty.tcp {
            hostname = localhost
            port = 0
          }
        }
      }
      """.stripMargin)

  logger.info(s"Starting actor system test@localhost:0")
  val system = ActorSystem("client", akkaConfig)



}
