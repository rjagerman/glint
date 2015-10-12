package glint

import akka.actor.{Actor, ActorSystem, ActorRef, Address, Props, Deploy}
import akka.remote.RemoteScope
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.messages.master.{ServerList, Register}

/**
 * The manager that handles the setup of parameter server actors
 *
 * @param config The configuration
 */
class Master(config: Config, system: ActorSystem) extends Actor with StrictLogging {

  /**
   * Collection of servers available
   */
  var servers = Set.empty[ActorRef]

  /**
   * Spawns a parameter server
   *
   * @param host The server hostname
   * @param port The port
   * @param systemName The actor system name to spawn in
   * @return The reference to the remote actor representing the parameter server
   */
  private def spawnParameterServer(host: String, port: Int, systemName: String): ActorRef = {
    logger.info(s"Starting parameter server ${systemName}@${host}:${port}")
    val address = Address("akka.tcp", systemName, host, port)
    system.actorOf(Props[Server].withDeploy(Deploy(scope = RemoteScope(address))))
  }

  override def receive: Receive = {

    case Register() =>
      logger.info(s"Registering server ${sender.path.toString}")
      servers = servers + sender
      sender ! true

    case ServerList() =>
      logger.info(s"Sending current server list")
      sender ! servers.toArray

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
   */
  def run(config: Config): Unit = {

    logger.debug("Parsing Akka configuration")
    val akkaConfig: Config = ConfigFactory.parseString(
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
            hostname = ${config.getString("glint.master.host")}
            port = ${config.getInt("glint.master.port")}
          }
        }
      }
      """.stripMargin)

    logger.debug("Starting master actor system")
    val system = ActorSystem(config.getString("glint.master.system"), akkaConfig)

    logger.debug("Starting master")
    val pm = system.actorOf(Props(classOf[Master], config, system), config.getString("glint.master.name"))

    logger.info("Master successfully started")

  }
}
