package glint

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.util.Timeout
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import glint.messages.master._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * The master that registers the available parameter servers and clients
  */
private[glint] class Master() extends Actor with ActorLogging {

  /**
    * Collection of servers available
    */
  var servers = Set.empty[ActorRef]

  /**
    * Collection of clients
    */
  var clients = Set.empty[ActorRef]

  override def receive: Receive = {

    case RegisterServer(server) =>
      log.info(s"Registering server ${server.path.toString}")
      servers += server
      context.watch(server)
      sender ! true

    case RegisterClient(client) =>
      log.info(s"Registering client ${sender.path.toString}")
      clients += client
      context.watch(client)
      sender ! true

    case ServerList() =>
      log.info(s"Sending current server list to ${sender.path.toString}")
      sender ! servers.toArray

    case ClientList() =>
      log.info(s"Sending current client list to ${sender.path.toString}")
      sender ! clients.toArray

    case Terminated(actor) =>
      actor match {
        case server: ActorRef if servers contains server =>
          log.info(s"Removing server ${server.path.toString}")
          servers -= server

        case client: ActorRef if clients contains client =>
          log.info(s"Removing client ${client.path.toString}")
          clients -= client

        case actor: ActorRef =>
          log.warning(s"Received terminated notification for unknown actor ${actor.path.toString}")
      }

  }
}

/**
  * The master node that registers the available parameter servers and clients
  */
private[glint] object Master extends StrictLogging {

  /**
    * Starts a parameter server master node ready to receive commands
    *
    * @param config The configuration
    * @return A future containing the started actor system and reference to the master actor
    */
  def run(config: Config): Future[(ActorSystem, ActorRef)] = {

    logger.debug("Starting master actor system")
    val system = ActorSystem(config.getString("glint.master.system"), config.getConfig("glint.master"))

    logger.debug("Starting master")
    val master = system.actorOf(Props[Master], config.getString("glint.master.name"))

    implicit val timeout = Timeout(config.getDuration("glint.master.startup-timeout", TimeUnit.MILLISECONDS) milliseconds)
    implicit val ec = ExecutionContext.Implicits.global

    val address = Address("akka", config.getString("glint.master.system"), config.getString("glint.master.host"),
      config.getInt("glint.master.port"))

    system.actorSelection(master.path.toSerializationFormat).resolveOne().map {
      case a: ActorRef =>
        logger.info("Master successfully started")
        (system, master)
    }

  }
}
