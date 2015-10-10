package glint.models.array

import akka.actor._
import akka.remote.RemoteScope
import akka.pattern.ask
import akka.util.Timeout
import glint.messages.server.Pull
import scala.concurrent.duration._
import glint.{Client, Server}
import glint.messages.master.ServerList
import glint.models.BigModel
import glint.partitioning.{Partitioner, UniformPartitioner}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

/**
 * Implementation of a big model where the underlying data representation is an array
 *
 * @param default The default value
 * @param partitioner The partitioner for the key space
 * @tparam V The type of values to store
 */
class ArrayBigModel[V : ClassTag](val default: V,
                                  val partitioner: Partitioner[Long, ActorRef]) extends BigModel[Long, V] {

  /**
   * Asynchronous function to pull values from the big model
   *
   * @param keys The array of keys
   * @return A future for the array of returned values
   */
  override def pull(keys: Array[Long]): Unit = {
    implicit val ec = ExecutionContext.Implicits.global

    val pulls = keys.groupBy(partitioner.partition).map {
      case (partition, partitionKeys) =>
        ask(partition, Pull(partitionKeys))(Timeout(30 seconds))
    }

    Future.sequence(pulls).onSuccess {
      case l => println(l)
    }
  }

  /**
   * Asynchronous function to push values to the big model
   *
   * @param keys The array of keys
   * @param values The array of values
   * @return A future for the completion of the operation
   */
  override def push(keys: Array[Long], values: Array[V]): Unit = {

  }
}


object ArrayBigModel {

  /**
   * Creates a big model
   *
   * @param size The size of the model key space
   * @param default The default value to initialize with
   * @param client The client interface
   * @tparam V The type of the values to store
   * @return An array big model instance
   */
  def create[V : ClassTag](size: Long, default: V, client: Client) : ArrayBigModel[V] = {

    // Construct "AddressList" message and send to master
    val message = new ServerList()

    // Master response with the list of servers
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(10 seconds)
    val servers = Await.result(ask(client.master, message), timeout.duration).asInstanceOf[Array[ActorRef]]

    // Spawn partial models on each of the servers
    val models = servers.map(x => x.path.address).zipWithIndex.map {
      case (address, index) =>
        servers.foreach(println)
        val start = Math.floor(size.toDouble * (index.toDouble / servers.length.toDouble)).toLong
        val end = Math.floor(size.toDouble * ((index.toDouble + 1) / servers.length.toDouble)).toLong

        println(address)

        val prop = Props(classOf[ArrayPartialModel[V]], start, end, default)
        client.system.actorOf(prop.withDeploy(Deploy(scope = RemoteScope(address))))
    }

    // Construct a corresponding partitioner
    val partitioner = new UniformPartitioner[ActorRef](models, size)

    // Construct an ArrayBigModel object with the appropriate partitioner
    new ArrayBigModel[V](default, partitioner)

  }

}

