package glint

import akka.actor.{Props, ActorSystem, Actor}
import akka.actor.Actor.Receive
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.messages.{Pull, Push, Register}

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
class ParameterServer extends Actor with StrictLogging {

  /**
   * The data
   */
  //val data = Array.fill[V](size)(construct)

  override def receive: Receive = {

    /**
     * Pull operation
     * This method will send communication back to the original sender with the requested information
     */
    case Pull(keys) =>
      logger.debug(s"Received key pull request from ${sender().path}")
      sender ! "'some pulled data...'"

  }

}

/**
 * Parameter server object
 */
object ParameterServer extends StrictLogging {

  /**
   * Starts a parameter server ready to receive commands
   *
   * @param host The host name
   * @param port The port
   */
  def run(config: Config, host: String, port: Int): Unit = {

    val name = "ParameterServer"
    logger.info(s"Starting actor system ${name}@${host}:${port}")

    logger.debug("Creating akka remote configuration")
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
            port = ${port}
            hostname = ${host}
          }
        }
      }
      """.stripMargin)
    val system = ActorSystem(name, akkaConfig)

    logger.info("Starting parameter server actor")
    val ps = system.actorOf(Props(new ParameterServer()))

    logger.debug("Reading master information from config")
    val masterHost = config.getString("glint.master.host")
    val masterPort = config.getInt("glint.master.port")
    val masterName = config.getString("glint.master.name")
    val masterSystem = config.getString("glint.master.system")

    logger.info(s"Registering with parameter manager ${masterHost}:${masterPort}")
    val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")
    master.tell(Register(host, port, name), ps)

  }
}
