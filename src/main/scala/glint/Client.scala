package glint

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.util.Timeout
import breeze.linalg.Vector
import com.typesafe.config.{Config, ConfigFactory}
import glint.exceptions.ModelCreationException
import glint.indexing.{CyclicIndexer, Indexer}
import glint.messages.master.{GetModel, RegisterClient, RegisterModel, ServerList}
import glint.models.client.{AsyncBigModel, BigModel}
import glint.models.server.{ScalarArrayPartialModel, VectorArrayPartialModel}
import glint.partitioning.{Partitioner, UniformPartitioner}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A client interface that facilitates easy communication with the master and provides easy-to-use functions to spawn
  * large models on the parameter servers.
  *
  * A typical usage scenario (create a distributed dense array with 10000 values initialized to 0.0 and pull/push it):
  *
  * {{{
  *   val client = Client()
  *   val bigModel = client.dense[Double]("somename", 10000, 0.0)
  *
  *   bigModel.pull(Array(1, 2, 5000, 8000)).onSuccess { case values => println(values.mkString(", ")) }
  *   bigModel.push(Array(1, 2, 300, 40), Array(0.1, 0.2, 300.2, 0.001))
  * }}}
  *
  * @constructor Create a new client with optional configuration (see glint.conf for an example)
  * @param config The configuration
  * @param system The actor system
  * @param master An actor reference to the master
  */
class Client(val config: Config, val system: ActorSystem, val master: ActorRef) {

  private implicit val timeout = Timeout(config.getDuration("glint.client.default-timeout", TimeUnit.MILLISECONDS) milliseconds)
  private implicit val ec = ExecutionContext.Implicits.global

  private[glint] val actor = system.actorOf(Props[ClientActor])
  private[glint] val registration = master ? RegisterClient(actor)

  /**
    * Gets a model with given identifier from the master
    *
    * @param id The identifier
    * @tparam K The type of keys for this model
    * @tparam V The type of values for this model
    * @return
    */
  def get[K, V](id: String): Future[BigModel[K, V]] = {
    (master ? new GetModel(id)).mapTo[Option[BigModel[K, V]]].map {
      case None => throw new NoSuchElementException(s"Model ${id} does not exist")
      case Some(x) => x
    }.mapTo[BigModel[K, V]]
  }

  /**
    * Constructs a distributed dense array (indexed by Long) containing algebraic semiring values (e.g. Double, Int, ...)
    *
    * Typical usage:
    * {{{
    *   client.denseScalarModel[Double]("name", 10000, 0.0)
    *   val model = client.get[Long, Double]("name")
    * }}}
    *
    * @param id The identifier
    * @param size The total size
    * @param default The default value to populate the model with
    * @tparam V The type of values to store
    * @return A future reference BigModel
    */
  def denseScalarModel[V: spire.algebra.Semiring : ClassTag](id: String,
                                                             size: Long,
                                                             default: V): Future[BigModel[Long, V]] = {
    create[Long, V](id,
      size,
      default,
      (models) => new CyclicIndexer(models.length, size),
      (models) => new UniformPartitioner[ActorRef](models, size),
      (start, end, default) => Props(classOf[ScalarArrayPartialModel[V]],
        start,
        end,
        default,
        implicitly[spire.algebra.Semiring[V]],
        implicitly[ClassTag[V]]))
  }

  /**
    * Constructs a distributed dense array (indexed by Long) containing breeze vectors
    *
    * Typical usage:
    * {{{
    *   client.denseVectorModel[DenseVector[Double]]("name", 10000, DenseVector.zeros[Double](20))
    *   val model = client.get[Long, DenseVector[Double]]("name")
    * }}}
    *
    * @param id The identifier
    * @param size The total size
    * @param default The default value to populate the model with
    * @tparam V The type of values to store
    * @return A future reference BigModel
    */
  def denseVectorModel[V: breeze.math.Semiring : ClassTag](id: String,
                                                           size: Long,
                                                           default: Vector[V]): Future[BigModel[Long, Vector[V]]] = {
    create[Long, Vector[V]](id,
      size,
      default,
      (models) => new CyclicIndexer(models.length, size),
      (models) => new UniformPartitioner[ActorRef](models, size),
      (start, end, default) => Props(classOf[VectorArrayPartialModel[V]],
        start,
        end,
        default,
        implicitly[breeze.math.Semiring[V]],
        implicitly[ClassTag[V]]))
  }

  /**
    * Constructs a big model distributed over multiple machines
    *
    * @param id The identifier
    * @param size The size of the big model (i.e. number of keys)
    * @param default The default value to store
    * @param indexer A function that creates an indexer based on a list of models
    * @param partitioner A function that creates a partitioner based on a list of models
    * @param props A function that creates an Akka Props object to construct partial models remotely
    * @tparam K The key type to store
    * @tparam V The value type to store
    * @return A future BigModel capable of referring to the machines storing the data
    */
  private def create[K: ClassTag, V: ClassTag](id: String,
                                               size: Long,
                                               default: V,
                                               indexer: (Array[ActorRef]) => Indexer[K],
                                               partitioner: (Array[ActorRef]) => Partitioner[ActorRef],
                                               props: (Long, Long, V) => Props): Future[BigModel[K, V]] = {

    // Get a list of servers
    val listOfServers = master ? new ServerList()

    // Spawn models on the servers and get a list of the models
    val listOfModels = listOfServers.mapTo[Array[ActorRef]].map { servers =>
      val nrOfServers = Math.min(size, servers.length).toInt
      if (nrOfServers <= 0) {
        throw new ModelCreationException("Cannot create a model with 0 parameter servers")
      }
      servers.take(nrOfServers).zipWithIndex.map {
        case (server, index) =>
          val start = Math.ceil(index * (size.toDouble / nrOfServers.toDouble)).toLong
          val end = Math.ceil((index+1) * (size.toDouble / nrOfServers.toDouble)).toLong
          val propsToDeploy = props(start, end, default)
          system.actorOf(propsToDeploy.withDeploy(Deploy(scope = RemoteScope(server.path.address))))
      }
    }

    // Map the list of models to a single BigModel reference
    val bigModel = listOfModels.map {
      case models: Array[ActorRef] => new AsyncBigModel[K, V](partitioner(models), indexer(models), models, default)
    }

    // Register the big model on the master before returning it
    bigModel.flatMap(m => (master ? RegisterModel(id, m, actor)).mapTo[BigModel[K, V]])

  }

  /**
    * Stops the glint client
    */
  def stop(): Unit = {
    system.shutdown()
  }

}

object Client {

  /**
    * Constructs a client asynchronously that succeeds once a client has registered with a master
    *
    * @param config The configuration
    * @return A future Client
    */
  def apply(config: Config): Future[Client] = {
    val default = ConfigFactory.parseResourcesAnySyntax("glint")
    val conf = config.withFallback(default).resolve()
    start(conf)
  }

  /**
    * Implementation to start a client by constructing an ActorSystem and establishing a connection to a master. It
    * creates the Client object and checks if its registration actually succeeds
    *
    * @param config The configuration
    * @return The future client
    */
  private def start(config: Config): Future[Client] = {

    // Get information from config
    val masterHost = config.getString("glint.master.host")
    val masterPort = config.getInt("glint.master.port")
    val masterName = config.getString("glint.master.name")
    val masterSystem = config.getString("glint.master.system")

    // Construct system and reference to master
    val system = ActorSystem(config.getString("glint.client.system"), config.getConfig("glint.client"))
    val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")

    // Set up implicit values for concurrency
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(config.getDuration("glint.client.default-timeout", TimeUnit.MILLISECONDS) milliseconds)

    // Resolve master node asynchronously
    val masterFuture = master.resolveOne()

    // Construct client based on resolved master asynchronously
    masterFuture.flatMap {
      case m =>
        val client = new Client(config, system, m)
        client.registration.map {
          case true => client
          case _ => throw new RuntimeException("Invalid client registration response from master")
        }
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
