package glint

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{ConfigValueFactory, Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.StrictLogging
import glint.Master._
import glint.messages.master.RegisterServer
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

/**
 * A parameter server
 */
class Server extends Actor with ActorLogging {

  override def receive: Receive = {

    case x =>
      log.warning(s"Received unknown message of type ${x.getClass}")
  }
}

/**
 * Parameter server object
 */
object Server extends StrictLogging {

  /**
   * Starts a parameter server ready to receive commands
   *
   * @param config The configuration
   * @param host The host name
   * @param port The port
   */
  def run(config: Config, host: String, port: Int): Future[(ActorSystem, ActorRef)] = {

    var modifiedConfig = config.withValue("glint.server.akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(host))
    modifiedConfig = modifiedConfig.withValue("glint.server.akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port))

    logger.debug(s"Starting actor system ${config.getString("glint.server.system")}@${host}:${port}")
    val system = ActorSystem(config.getString("glint.server.system"), modifiedConfig.getConfig("glint.server"))

    logger.debug("Starting server actor")
    val server = system.actorOf(Props[Server], config.getString("glint.server.name"))

    logger.debug("Reading master information from config")
    val masterHost = config.getString("glint.master.host")
    val masterPort = config.getInt("glint.master.port")
    val masterName = config.getString("glint.master.name")
    val masterSystem = config.getString("glint.master.system")

    logger.info(s"Registering with master ${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(config.getDuration("glint.server.registration-timeout", TimeUnit.MILLISECONDS) milliseconds)
    val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")
    val registration = master ? RegisterServer(server)

    registration.map {
      case a =>
        logger.info("Server successfully registered with master")
        (system, server)
    }

    /*registration.transform {
      case a: ActorRef =>
        logger.info(master.path.toSerializationFormatWithAddress(address))
        logger.info("Master successfully started")
        (system, server)
    }

    registration.onSuccess {
      case true =>
        logger.info(s"Succesfully registered with master")
      case _ =>
        logger.error(s"Unknown response from master")
        system.shutdown()
    }

    registration.onFailure {
      case e: Exception =>
        logger.error(s"Unable to register with master")
        logger.error(e.getMessage)
        system.shutdown()
    }*/

  }
}
