package glint

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import glint.exceptions.ModelCreationException
import glint.indexing.{CyclicIndexer, Indexer}
import glint.messages.master.{RegisterClient, ServerList}
import glint.models.client.async._
import glint.models.client.{BigMatrix, BigVector}
import glint.models.server._
import glint.partitioning.{Partitioner, RangePartitioner}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.runtime.universe.{TypeTag, typeOf}

/**
  * The client provides the functions needed to spawn large distributed matrices and vectors on the parameter servers.
  * Use the companion object to construct a Client object from a configuration file.
  *
  * @constructor Use the companion object to construct a Client object
  * @param config The configuration
  * @param system The actor system
  * @param master An actor reference to the master
  */
class Client(val config: Config,
                       private[glint] val system: ActorSystem,
                       private[glint] val master: ActorRef) {

  private implicit val timeout = Timeout(config.getDuration("glint.client.timeout", TimeUnit.MILLISECONDS) milliseconds)
  private implicit val ec = ExecutionContext.Implicits.global

  private[glint] val actor = system.actorOf(Props[ClientActor])
  private[glint] val registration = master ? RegisterClient(actor)

  /**
    * Constructs a distributed matrix (indexed by (row: Long, col: Int)) for specified type of values
    *
    * @param rows The number of rows
    * @param cols The number of columns
    * @param modelsPerServer The number of partial models to store per parameter server (default: 1)
    * @tparam V The type of values to store, must be one of the following: Int, Long, Double or Float
    * @return The constructed [[glint.models.client.BigMatrix BigMatrix]]
    */
  def matrix[V: breeze.math.Semiring : TypeTag](rows: Long, cols: Int, modelsPerServer: Int = 1): BigMatrix[V] = {
    matrix[V](rows,
      cols,
      modelsPerServer,
      (models: Array[ActorRef]) => new CyclicIndexer(models.length, rows),
      (models: Array[ActorRef]) => new RangePartitioner[ActorRef](models, rows))
  }

  /**
    * Constructs a distributed matrix (indexed by (row: Long, col: Int)) for specified type of values
    *
    * @param rows The number of rows
    * @param cols The number of columns
    * @param modelsPerServer The number of partial models to store per parameter server
    * @param indexer A function that creates an [[glint.indexing.Indexer indexer]] that indexes keys into a new space
    * @param partitioner A function that creates a [[glint.partitioning.Partitioner partitioner]] that partitions keys
    *                    onto parameter servers
    * @tparam V The type of values to store, must be one of the following: Int, Long, Double or Float
    * @return The constructed [[glint.models.client.BigMatrix BigMatrix]]
    */
  def matrix[V: breeze.math.Semiring : TypeTag](rows: Long,
                                                cols: Int,
                                                modelsPerServer: Int,
                                                indexer: (Array[ActorRef]) => Indexer[Long],
                                                partitioner: (Array[ActorRef]) => Partitioner[ActorRef]): BigMatrix[V] = {

    // Get a list of servers
    val listOfServers = master ? new ServerList()

    // Spawn models on the servers and get a list of the models
    val listOfModels = listOfServers.mapTo[Array[ActorRef]].map { servers =>
      val nrOfServers = Math.min(rows, servers.length).toInt
      if (nrOfServers <= 0) {
        throw new ModelCreationException("Cannot create a model with 0 parameter servers")
      }
      val nrOfModels = Math.min(nrOfServers * modelsPerServer, rows).toInt
      val models = new Array[ActorRef](nrOfModels)
      models.zipWithIndex.foreach { case (_, i) => models(i) = servers(i % nrOfServers) }
      models.take(nrOfModels).zipWithIndex.map {
        case (server, index) =>
          val start = Math.ceil(index * (rows.toDouble / nrOfModels.toDouble)).toLong
          val end = Math.ceil((index + 1) * (rows.toDouble / nrOfModels.toDouble)).toLong
          val propsToDeploy = implicitly[TypeTag[V]].tpe match {
            case x if x <:< typeOf[Int] => Props(classOf[PartialMatrixInt], start, end, cols)
            case x if x <:< typeOf[Long] => Props(classOf[PartialMatrixLong], start, end, cols)
            case x if x <:< typeOf[Float] => Props(classOf[PartialMatrixFloat], start, end, cols)
            case x if x <:< typeOf[Double] => Props(classOf[PartialMatrixDouble], start, end, cols)
            case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
          }
          system.actorOf(propsToDeploy.withDeploy(Deploy(scope = RemoteScope(server.path.address))))
      }
    }

    // Map the list of models to a single BigModel reference
    val bigModelFuture = listOfModels.map {
      case models: Array[ActorRef] =>
        implicitly[TypeTag[V]].tpe match {
          case x if x <:< typeOf[Int] => new AsyncBigMatrixInt(partitioner(models), indexer(models), config, rows, cols).asInstanceOf[BigMatrix[V]]
          case x if x <:< typeOf[Long] => new AsyncBigMatrixLong(partitioner(models), indexer(models), config, rows, cols).asInstanceOf[BigMatrix[V]]
          case x if x <:< typeOf[Float] => new AsyncBigMatrixFloat(partitioner(models), indexer(models), config, rows, cols).asInstanceOf[BigMatrix[V]]
          case x if x <:< typeOf[Double] => new AsyncBigMatrixDouble(partitioner(models), indexer(models), config, rows, cols).asInstanceOf[BigMatrix[V]]
          case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
        }
    }
    Await.result(bigModelFuture, config.getDuration("glint.client.timeout", TimeUnit.MILLISECONDS) milliseconds)

  }

  /**
    * Constructs a distributed vector (indexed by key: Long) for specified type of values
    *
    * @param keys The number of rows
    * @param modelsPerServer The number of partial models to store per parameter server (default: 1)
    * @tparam V The type of values to store, must be one of the following: Int, Long, Double or Float
    * @return The constructed [[glint.models.client.BigVector BigVector]]
    */
  def vector[V: breeze.math.Semiring : TypeTag](keys: Long, modelsPerServer: Int = 1): BigVector[V] = {
    vector[V](keys,
      modelsPerServer,
      (models: Array[ActorRef]) => new CyclicIndexer(models.length, keys),
      (models: Array[ActorRef]) => new RangePartitioner[ActorRef](models, keys))
  }

  /**
    * Constructs a distributed vector (indexed by key: Long) for specified type of values
    *
    * @param keys The number of keys
    * @param modelsPerServer The number of partial models to store per parameter server
    * @param indexer A function that creates an [[glint.indexing.Indexer indexer]] that indexes keys into a new space
    * @param partitioner A function that creates a [[glint.partitioning.Partitioner partitioner]] that partitions keys
    *                    onto parameter servers
    * @tparam V The type of values to store, must be one of the following: Int, Long, Double or Float
    * @return The constructed [[glint.models.client.BigVector BigVector]]
    */
  def vector[V: breeze.math.Semiring : TypeTag](keys: Long,
                                                modelsPerServer: Int,
                                                indexer: (Array[ActorRef]) => Indexer[Long],
                                                partitioner: (Array[ActorRef]) => Partitioner[ActorRef]): BigVector[V] = {

    // Get a list of servers
    val listOfServers = master ? new ServerList()

    // Spawn models on the servers and get a list of the models
    val listOfModels = listOfServers.mapTo[Array[ActorRef]].map { servers =>
      val nrOfServers = Math.min(keys, servers.length).toInt
      if (nrOfServers <= 0) {
        throw new ModelCreationException("Cannot create a model with 0 parameter servers")
      }
      val nrOfModels = Math.min(nrOfServers * modelsPerServer, keys).toInt
      val models = new Array[ActorRef](nrOfModels)
      models.zipWithIndex.foreach { case (_, i) => models(i) = servers(i % nrOfServers) }
      models.take(nrOfModels).zipWithIndex.map {
        case (server, index) =>
          val start = Math.ceil(index * (keys.toDouble / nrOfModels.toDouble)).toLong
          val end = Math.ceil((index + 1) * (keys.toDouble / nrOfModels.toDouble)).toLong
          val propsToDeploy = implicitly[TypeTag[V]].tpe match {
            case x if x <:< typeOf[Int] => Props(classOf[PartialVectorInt], start, end)
            case x if x <:< typeOf[Long] => Props(classOf[PartialVectorLong], start, end)
            case x if x <:< typeOf[Float] => Props(classOf[PartialVectorFloat], start, end)
            case x if x <:< typeOf[Double] => Props(classOf[PartialVectorDouble], start, end)
            case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
          }
          system.actorOf(propsToDeploy.withDeploy(Deploy(scope = RemoteScope(server.path.address))))
      }
    }

    // Map the list of models to a single BigModel reference
    val bigModelFuture = listOfModels.map {
      case models: Array[ActorRef] =>
        implicitly[TypeTag[V]].tpe match {
          case x if x <:< typeOf[Int] => new AsyncBigVectorInt(partitioner(models), indexer(models), config, keys).asInstanceOf[BigVector[V]]
          case x if x <:< typeOf[Long] => new AsyncBigVectorLong(partitioner(models), indexer(models), config, keys).asInstanceOf[BigVector[V]]
          case x if x <:< typeOf[Float] => new AsyncBigVectorFloat(partitioner(models), indexer(models), config, keys).asInstanceOf[BigVector[V]]
          case x if x <:< typeOf[Double] => new AsyncBigVectorDouble(partitioner(models), indexer(models), config, keys).asInstanceOf[BigVector[V]]
          case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
        }
    }
    Await.result(bigModelFuture, config.getDuration("glint.client.timeout", TimeUnit.MILLISECONDS) milliseconds)
  }

  /**
    * Stops the glint client
    */
  def stop(): Unit = {
    system.shutdown()
  }

}

/**
  * Contains functions to easily create a client object that is connected to the glint cluster.
  *
  * You can construct a client with a specific configuration:
  * {{{
  *   import glint.Client
  *
  *   import java.io.File
  *   import com.typesafe.config.ConfigFactory
  *
  *   val config = ConfigFactory.parseFile(new File("/your/file.conf"))
  *   val client = Client(config)
  * }}}
  *
  * The resulting client object can then be used to create distributed matrices or vectors on the available parameter
  * servers:
  * {{{
  *   val matrix = client.matrix[Double](10000, 50)
  * }}}
  */
object Client {

  /**
    * Constructs a client
    *
    * @param config The configuration
    * @return A future Client
    */
  def apply(config: Config): Client = {
    val default = ConfigFactory.parseResourcesAnySyntax("glint")
    val conf = config.withFallback(default).resolve()
    Await.result(start(conf), config.getDuration("glint.client.timeout", TimeUnit.MILLISECONDS) milliseconds)
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
    implicit val timeout = Timeout(config.getDuration("glint.client.timeout", TimeUnit.MILLISECONDS) milliseconds)

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
  * The client actor class. The master keeps a death watch on this actor and knows when it is terminated.
  *
  * This actor either gets terminated when the system shuts down (e.g. when the Client object is destroyed) or when it
  * crashes unexpectedly.
  */
private class ClientActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case x => log.info(s"Client actor received message ${x}")
  }
}
