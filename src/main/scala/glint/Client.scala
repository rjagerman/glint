package glint

import java.io.File

import akka.actor.{ActorSystem, Actor}
import akka.actor.Actor.Receive
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.Glint._
import glint.messages.server.Response

/**
 * A client interface that facilitates easy communication with the master
 *
 * @param config The configuration
 */
class Client(config: Config = ConfigFactory.parseFile(new File(getClass.getClassLoader.getResource("default.conf").getFile))) extends StrictLogging {

  private val akkaConfig = ConfigFactory.parseString(
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
            hostname = ${config.getString("glint.client.host")}
            port = ${config.getInt("glint.client.port")}
          }
        }
      }
      """.stripMargin)

  private val masterHost = config.getString("glint.master.host")
  private val masterPort = config.getInt("glint.master.port")
  private val masterName = config.getString("glint.master.name")
  private val masterSystem = config.getString("glint.master.system")

  @transient
  lazy val system = ActorSystem(config.getString("glint.client.system"), akkaConfig)

  @transient
  lazy val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")

}
