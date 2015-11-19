package glint

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigValueFactory}
import com.typesafe.scalalogging.slf4j.StrictLogging
import glint.messages.master.RegisterServer

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

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
    */
  def run(config: Config): Future[(ActorSystem, ActorRef)] = {

    logger.debug(s"Starting actor system ${config.getString("glint.server.system")}")
    val system = ActorSystem(config.getString("glint.server.system"), config.getConfig("glint.server"))

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

  }
}
