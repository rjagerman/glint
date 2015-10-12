package glint

import akka.actor.{Actor, Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.messages.master.Register
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

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

    logger.debug(s"Starting actor system ${config.getString("glint.worker.system")}@${host}:${port}")
    val system = ActorSystem(config.getString("glint.worker.system"), akkaConfig)

    logger.debug("Starting server actor")
    val ps = system.actorOf(Props[Server])

    logger.debug("Reading master information from config")
    val masterHost = config.getString("glint.master.host")
    val masterPort = config.getInt("glint.master.port")
    val masterName = config.getString("glint.master.name")
    val masterSystem = config.getString("glint.master.system")

    logger.info(s"Registering with master ${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")
    val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")

    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(10 seconds)
    val registration = ask(master, Register())
    registration.onSuccess {
      case true =>
        logger.info(s"Succesfully registered with master")
      case _ =>
        logger.error(s"Unknown response from master")
        system.terminate()
    }
    registration.onFailure {
      case e: Exception =>
        logger.error(s"Unable to register with master")
        logger.error(e.getMessage)
        system.terminate()
    }

  }
}
