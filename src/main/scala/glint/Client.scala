package glint

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import glint.exceptions.ModelCreationException
import glint.messages.master.{RegisterClient, ServerList}
import glint.models.client.async._
import glint.models.client.{BigMatrix, BigVector}
import glint.models.server._
import glint.partitioning.cyclic.CyclicPartitioner
import glint.partitioning.range.RangePartitioner
import glint.partitioning.{Partition, Partitioner}

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
    * Creates a distributed model on the parameter servers
    *
    * @param keys The total number of keys
    * @param modelsPerServer The number of models to spawn per parameter server
    * @param createPartitioner A function that creates a partitioner based on a number of keys and partitions
    * @param generateServerProp A function that generates a server prop of a partial model for a particular partition
    * @param generateClientObject A function that generates a client object based on the partitioner and spawned models
    * @tparam M The final model type to generate
    * @return The generated model
    */
  private def create[M](keys: Long,
                        modelsPerServer: Int,
                        createPartitioner: (Int, Long) => Partitioner,
                        generateServerProp: Partition => Props,
                        generateClientObject: (Partitioner, Array[ActorRef], Config) => M): M = {

    // Get a list of servers
    val listOfServers = serverList()

    // Construct a big model based on the list of servers
    val bigModelFuture = listOfServers.map { servers =>

      // Check if there are servers online
      if (servers.isEmpty) {
        throw new ModelCreationException("Cannot create a model without active parameter servers")
      }

      // Construct a partitioner
      val numberOfPartitions = Math.min(keys, modelsPerServer * servers.length).toInt
      val partitioner = createPartitioner(numberOfPartitions, keys)
      val partitions = partitioner.all()

      // Spawn models that are deployed on the parameter servers according to the partitioner
      val models = new Array[ActorRef](numberOfPartitions)
      var partitionIndex = 0
      while (partitionIndex < numberOfPartitions) {
        val serverIndex = partitionIndex % servers.length
        val server = servers(serverIndex)
        val partition = partitions(partitionIndex)
        val prop = generateServerProp(partition)
        models(partitionIndex) = system.actorOf(prop.withDeploy(Deploy(scope = RemoteScope(server.path.address))))
        partitionIndex += 1
      }

      // Construct a big model client object
      generateClientObject(partitioner, models, config)
    }

    // Wait for the big model to finish
    Await.result(bigModelFuture, config.getDuration("glint.client.timeout", TimeUnit.MILLISECONDS) milliseconds)

  }

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
    matrix[V](rows, cols, modelsPerServer, (partitions: Int, keys: Long) => RangePartitioner(partitions, keys))
  }

  /**
    * Constructs a distributed matrix (indexed by (row: Long, col: Int)) for specified type of values
    *
    * @param rows The number of rows
    * @param cols The number of columns
    * @param modelsPerServer The number of partial models to store per parameter server
    * @param createPartitioner A function that creates a [[glint.partitioning.Partitioner partitioner]] that partitions
    *                          keys
    * @tparam V The type of values to store, must be one of the following: Int, Long, Double or Float
    * @return The constructed [[glint.models.client.BigMatrix BigMatrix]]
    */
  def matrix[V: breeze.math.Semiring : TypeTag](rows: Long,
                                                cols: Int,
                                                modelsPerServer: Int,
                                                createPartitioner: (Int, Long) => Partitioner): BigMatrix[V] = {

    val propFunction = numberType[V] match {
      case "Int" => (partition: Partition) => Props(classOf[PartialMatrixInt], partition, cols)
      case "Long" => (partition: Partition) => Props(classOf[PartialMatrixLong], partition, cols)
      case "Float" => (partition: Partition) => Props(classOf[PartialMatrixFloat], partition, cols)
      case "Double" => (partition: Partition) => Props(classOf[PartialMatrixDouble], partition, cols)
      case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
    }

    val objFunction = numberType[V] match {
      case "Int" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigMatrixInt(partitioner, models, config, rows, cols).asInstanceOf[BigMatrix[V]]
      case "Long" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigMatrixLong(partitioner, models, config, rows, cols).asInstanceOf[BigMatrix[V]]
      case "Float" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigMatrixFloat(partitioner, models, config, rows, cols).asInstanceOf[BigMatrix[V]]
      case "Double" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigMatrixDouble(partitioner, models, config, rows, cols).asInstanceOf[BigMatrix[V]]
      case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
    }

    create[BigMatrix[V]](rows, modelsPerServer, createPartitioner, propFunction, objFunction)
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
    vector[V](keys, modelsPerServer, (partitions: Int, keys: Long) => RangePartitioner(partitions, keys))
  }

  /**
    * Constructs a distributed vector (indexed by key: Long) for specified type of values
    *
    * @param keys The number of keys
    * @param modelsPerServer The number of partial models to store per parameter server
    * @param createPartitioner A function that creates a [[glint.partitioning.Partitioner partitioner]] that partitions
    *                          keys
    * @tparam V The type of values to store, must be one of the following: Int, Long, Double or Float
    * @return The constructed [[glint.models.client.BigVector BigVector]]
    */
  def vector[V: breeze.math.Semiring : TypeTag](keys: Long,
                                                modelsPerServer: Int,
                                                createPartitioner: (Int, Long) => Partitioner): BigVector[V] = {

    val propFunction = numberType[V] match {
      case "Int" => (partition: Partition) => Props(classOf[PartialVectorInt], partition)
      case "Long" => (partition: Partition) => Props(classOf[PartialVectorLong], partition)
      case "Float" => (partition: Partition) => Props(classOf[PartialVectorFloat], partition)
      case "Double" => (partition: Partition) => Props(classOf[PartialVectorDouble], partition)
      case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
    }

    val objFunction = numberType[V] match {
      case "Int" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigVectorInt(partitioner, models, config, keys).asInstanceOf[BigVector[V]]
      case "Long" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigVectorLong(partitioner, models, config, keys).asInstanceOf[BigVector[V]]
      case "Float" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigVectorFloat(partitioner, models, config, keys).asInstanceOf[BigVector[V]]
      case "Double" => (partitioner: Partitioner, models: Array[ActorRef], config: Config) =>
        new AsyncBigVectorDouble(partitioner, models, config, keys).asInstanceOf[BigVector[V]]
      case x => throw new ModelCreationException(s"Cannot create model for unsupported value type $x")
    }

    create[BigVector[V]](keys, modelsPerServer, createPartitioner, propFunction, objFunction)

  }

  /**
    * @return A future containing an array of available servers
    */
  def serverList(): Future[Array[ActorRef]] = {
    (master ? new ServerList()).mapTo[Array[ActorRef]]
  }

  /**
    * Determines number type at runtime of given type tag
    *
    * @tparam V The type to infer
    * @return The string representation of the type
    */
  def numberType[V: TypeTag]: String = {
    implicitly[TypeTag[V]].tpe match {
      case x if x <:< typeOf[Int] => "Int"
      case x if x <:< typeOf[Long] => "Long"
      case x if x <:< typeOf[Float] => "Float"
      case x if x <:< typeOf[Double] => "Double"
      case x => s"${x.toString}"
    }
  }

  /**
    * Stops the glint client
    */
  def stop(): Unit = {
    system.terminate()
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
    * Constructs a client with the default configuration
    *
    * @return The client
    */
  def apply(): Client = {
    this(ConfigFactory.empty())
  }

  /**
    * Constructs a client
    *
    * @param config The configuration
    * @return A future Client
    */
  def apply(config: Config): Client = {
    val default = ConfigFactory.parseResourcesAnySyntax("glint")
    val conf = config.withFallback(default).resolve()
    Await.result(start(conf), conf.getDuration("glint.client.timeout", TimeUnit.MILLISECONDS) milliseconds)
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
