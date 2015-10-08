package glint

import akka.actor.Actor.Receive
import akka.actor._
import akka.remote.RemoteScope
import glint.messages.{CreateModel, Register}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.models.PartialModel
import glint.partitioning.ContiguousPartioner

import scala.reflect.ClassTag

/**
 * The manager that handles the setup of parameter server actors
 *
 * @param config The configuration
 */
class Master(config: Config, system: ActorSystem) extends Actor with StrictLogging {

  /**
   * Collection of servers available
   */
  var servers = scala.collection.mutable.Map[String, ActorRef]()

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

  override def receive: Actor.Receive = {

    /**
     * When a worker registers itself we store it
     */
    case Register(host, port, systemName) =>
      logger.info(sender().toString())
      servers(s"${host}${port}") = sender()

    case CreateModel(modelClass, size) =>
      logger.info(s"Creating a model with key space size ${size}")
      servers.values.zipWithIndex.foreach {
        case (server, i) =>
          // Spawn partial model at server i that takes key space from start - end
          val start = Math.floor(i * (size.toDouble / servers.size.toDouble)).toLong
          val end = Math.floor((i+1) * (size.toDouble / servers.size.toDouble)).toLong - 1
          val address = server.path.address

          system.actorOf(Props(modelClass, start, end))

      }


    /*case c:CreateModel[K : ClassTag, V : ClassTag] =>
      logger.info(s"Creating a model with key space size ${size}")

      servers.values.zipWithIndex.foreach {
        case (server, i) =>
          // Spawn partial model at server i that takes key space from start - end
          val start = Math.floor(i * (size.toDouble / servers.size.toDouble)).toLong
          val end = Math.floor((i+1) * (size.toDouble / servers.size.toDouble)).toLong - 1
          val address = server.path.address

          //system.actorOf(Props[PartialModel], start, end, model)

      }*/


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

    logger.info("Starting master")
    val pm = system.actorOf(Props(classOf[Master], config, system), config.getString("glint.master.name"))

  }
}
