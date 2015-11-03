package glint

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.remote.RemoteScope
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.StrictLogging
import glint.messages.master.{ClientList, RegisterClient, RegisterModel, ServerList, RegisterServer}
import glint.models.BigModel

/**
 * The manager that handles the setup of parameter server actors
 */
class Master() extends Actor with ActorLogging {

  /**
   * Collection of servers available
   */
  var servers = Set.empty[ActorRef]

  /**
   * Collection of models
   */
  var clientModels = Map.empty[ActorRef, Set[String]]

  /**
   * Collection of models
   */
  var models = Map.empty[String, (BigModel[_, _], ActorRef)]

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

    case RegisterModel(name, model, client) =>
      log.info(s"Registering model ${name}")
      models = models + (name -> (model, client))
      clientModels = clientModels + (client -> (clientModels.getOrElse(client, Set.empty[String]) + name))
      sender ! model

    case GetModel(name) =>
      log.info(s"Sending model reference ${name} to ${sender.path.toString}")
      sender ! models.get(name).map(_._1)

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
          log.debug(s"Removing models associated with client ${client.path.toString}")
          clientModels.getOrElse(client, Set.empty[String]).foreach {
            case name =>
              models(name)._1.destroy()
              models = models - name
          }
          clientModels = clientModels - client

        case actor: ActorRef =>
          log.warning(s"Received terminated notification for unknown actor ${actor.path.toString}")
      }

  }
}

/**
 * Parameter manager object
 */
object Master extends StrictLogging {

  /**
   * Starts a parameter server ready to receive commands
   *
   * @param config The configuration
   * @return The started actor system and reference to the master actor
   */
  def run(config: Config): Future[(ActorSystem, ActorRef)] = {

    logger.debug("Starting master actor system")
    val system = ActorSystem(config.getString("glint.master.system"), config.getConfig("glint.master"))

    logger.debug("Starting master")
    val master = system.actorOf(Props[Master], config.getString("glint.master.name"))

    implicit val timeout = Timeout(config.getDuration("glint.master.startup-timeout", TimeUnit.MILLISECONDS) milliseconds)
    implicit val ec = ExecutionContext.Implicits.global

    val address = Address("akka.tcp", config.getString("glint.master.system"), config.getString("glint.master.host"),
      config.getInt("glint.master.port"))

    system.actorSelection(master.path.toSerializationFormat).resolveOne().map {
      case a: ActorRef =>
        logger.info("Master successfully started")
        (system, master)
    }

  }
}
