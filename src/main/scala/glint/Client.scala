package glint

import java.util.concurrent.TimeUnit

import akka.actor.Actor.Receive
import akka.actor._
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.{LazyLogging, StrictLogging}
import glint.messages.master.{ServerList, RegisterClient, RegisterModel}
import glint.models.BigModel
import glint.models.impl.ArrayPartialModel
import glint.partitioning.{UniformPartitioner, Partitioner}

import scala.concurrent.{Promise, ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * A client interface that facilitates easy communication with the master and provides easy-to-use functions to spawn
 * large models on the parameter servers.
 *
 * A typical usage scenario (create a distributed dense array with 10000 values initialized to 0.0 and pull/push it):
 *
 * {{{
 *   val client = new Client()
 *   val bigModel = client.dense[Double](10000, 0.0)
 *
 *   bigModel.pull(Array(1, 2, 5000, 8000)).onSuccess { case values => println(values.mkString(", ")) }
 *   bigModel.push(Array(1, 2, 300, 40), Array(0.1, 0.2, 300.2, 0.001))
 * }}}
 *
 * @constructor Create a new client with optional configuration (see glint.conf for an example)
 * @param config The configuration
 */
class Client(val config: Config) {

  private val clientSystem = config.getString("glint.client.system")
  private val masterHost = config.getString("glint.master.host")
  private val masterPort = config.getInt("glint.master.port")
  private val masterName = config.getString("glint.master.name")
  private val masterSystem = config.getString("glint.master.system")

  implicit val timeout = Timeout(config.getDuration("glint.client.default-timeout", TimeUnit.MILLISECONDS) milliseconds)
  implicit val ec = ExecutionContext.Implicits.global

  val system = ActorSystem(clientSystem, config.getConfig("glint.client"))
  val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")
  val client = system.actorOf(Props[ClientActor])

  /**
   * Gets a model with given identifier from the master
   *
   * @param id The identifier
   * @tparam K The type of keys for this model
   * @tparam V The type of values for this model
   * @return
   */
  def get[K, V](id: String): Future[Option[BigModel[K, V]]] = {
    (master ? new GetModel(id)).mapTo[Option[BigModel[K, V]]]
  }

  /**
   * Constructs a distributed dense array (indexed by Long) over the parameter servers
   * The returned model is serializable and safe to be used across machines (e.g. in Spark workers)
   *
   * Typical usage:
   * {{{
   *   client.dense[Double]("name", 10000, 0.0)
   *   val model = client.get[Long, Double]("name")
   * }}}
   *
   * @param id The identifier
   * @param size The total size
   * @param default The default value to populate the array with
   * @tparam V The type of values to store
   * @return A future reference BigModel
   */
  def dense[V : ClassTag](id: String, size: Long, default: V): Future[BigModel[Long, V]] = {
    create[Long, V](id,
      size,
      default,
      (models) => new UniformPartitioner[ActorRef](models, size),
      (start, end, default) => Props(classOf[ArrayPartialModel[V]], start, end, default, implicitly[ClassTag[V]]))
  }

  /**
   * Constructs a big model distributed over multiple machines
   *
   * @param id The identifier
   * @param size The size of the big model (i.e. number of keys)
   * @param default The default value to store
   * @param partitioner A function that creates a partitioner based on a list of models
   * @param props A function that creates an Akka Props object to construct partial models remotely
   * @tparam K The key type to store
   * @tparam V The value type to store
   * @return A future BigModel capable of referring to the machines storing the data
   */
  private def create[K : ClassTag, V : ClassTag](id: String,
                                                 size: Long,
                                                 default: V,
                                                 partitioner: (Array[ActorRef]) => Partitioner[K, ActorRef],
                                                 props: (Long, Long, V) => Props) : Future[BigModel[K, V]] = {

    // Get a list of servers
    val listOfServers = master ? new ServerList()

    // Spawn models on the servers and get a list of the models
    val listOfModels = listOfServers.map {
      case servers: Array[ActorRef] =>
        servers.zipWithIndex.map {
          case (server, index) =>
            val start = Math.floor(size.toDouble * (index.toDouble / servers.length.toDouble)).toLong
            val end = Math.floor(size.toDouble * ((index.toDouble + 1) / servers.length.toDouble)).toLong
            val propsToDeploy = props(start, end, default)
            system.actorOf(propsToDeploy.withDeploy(Deploy(scope = RemoteScope(server.path.address))))
            //(server ? propsToDeploy).mapTo[ActorRef]
        }
    }

    // Map the list of models to a single BigModel reference
    val bigModel = listOfModels.map {
      case models: Array[ActorRef] => new BigModel[K, V](partitioner(models), models, default)
    }

    // Register the big model on the master before returning it
    bigModel.flatMap(m => (master ? RegisterModel(id, m, client)).mapTo[BigModel[K, V]])

  }

  /**
   * Stops the glint client
   */
  def stop(): Unit = {
    system.shutdown()
  }

}

object Client {
  def apply(config: Config): Future[Client] = {
    val default = ConfigFactory.parseResourcesAnySyntax("glint").resolve()
    val client = new Client(config.withFallback(default).resolve())
    implicit val ec = client.ec
    implicit val timeout = Timeout(config.getDuration("glint.client.default-timeout", TimeUnit.MILLISECONDS) milliseconds)

    // Resolve master node
    val masterFuture = client.master.resolveOne()

    // Register client with master
    val registrationFuture = masterFuture.flatMap {
      case master: ActorRef => (master ? RegisterClient(client.client)).mapTo[Boolean]
    }

    // Check registration success response from master
    registrationFuture.map {
      case true => client
      case _ => throw new RuntimeException("Invalid client registration response from master")
    }
  }
}

/**
 * The client actor class. The master keeps a death watch on this actor and knows when it is terminated. If it is
 * terminated the master can release all associated resources (e.g. BigModels on parameter servers).
 *
 * This actor either gets terminated when the system shuts down (e.g. when the Client object is destroyed) or when it
 * crashes unexpectedly.
 */
private class ClientActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case x => log.info(s"Client actor received message ${x}")
  }
}
