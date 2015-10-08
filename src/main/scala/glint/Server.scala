package glint

import akka.actor.{Actor, Props, ActorSystem}
import akka.actor.Actor.Receive
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.messages.{Pull, Push, Register, Response}

import scala.reflect.ClassTag

/**
 * A parameter server which stores the parameters and handles pull and push requests
 *
 * @param size The number of keys to store on this parameter server
 * @param startIndex Starting index of keys stored on this parameter server
 * @param construct A constructor that builds the initial values
 * @param push Handler for push events
 * @param pull Handler for pull events
 */
/*class ParameterServer[V: ClassTag, P](size: Int,
                                  startIndex: Long,
                                  construct: => V,
                                  push: P => V,
                                  pull: Long => V) extends Actor {*/

/**
 * A parameter server
 */
class Server extends Actor with StrictLogging {
  override def receive: Receive = {
    case x =>
      logger.warn(s"Received unknown message of type ${x.getClass}")
  }
}



/**
 * Parameter server object
 */
object Server extends StrictLogging {

  /**
   * Starts a parameter server ready to receive commands
   *
   * @param host The host name
   * @param port The port
   */
  def run(config: Config, host: String, port: Int): Unit = {

    logger.debug("Creating akka remote configuration")
    val name = "ParameterServer"
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
            hostname = ${host}
            port = ${port}
          }
        }
      }
      """.stripMargin)

    logger.info(s"Starting actor system ${name}@${host}:${port}")
    val system = ActorSystem(name, akkaConfig)

    logger.info("Starting server actor")
    val ps = system.actorOf(Props[Server])

    logger.debug("Reading master information from config")
    val masterHost = config.getString("glint.master.host")
    val masterPort = config.getInt("glint.master.port")
    val masterName = config.getString("glint.master.name")
    val masterSystem = config.getString("glint.master.system")

    logger.info(s"Registering with master ${masterHost}:${masterPort}")
    val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")
    master.tell(Register(host, port, name), ps)

  }
}
