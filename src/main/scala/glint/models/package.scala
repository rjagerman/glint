package glint

import akka.actor.{Deploy, Props, ActorRef}
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.util.Timeout
import glint.messages.master.ServerList
import glint.models.impl.ArrayPartialModel
import glint.partitioning.{Partitioner, UniformPartitioner}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Defines common methods to construct big models
 */
package object models {

  /**
   * Constructs a dense big array distributed over multiple machines using a uniform keyspace partitioner
   *
   * @param size The size of the array
   * @param default The default value of the array
   * @param client The glint client
   * @tparam V The type of values to store in the array
   * @return A BigModel capable of referring to the array
   */
  def array[V : ClassTag](size: Long, default: V, client: Client): BigModel[Long, V] = {
    create[Long, V](size,
                    default,
                    client,
                    (models) => new UniformPartitioner[ActorRef](models, size),
                    (start, end, default) => Props(classOf[ArrayPartialModel[V]], start, end, default, implicitly[ClassTag[V]]))
  }

  /**
   * Constructs a big model distributed over multiple machines
   *
   * @param size The size of the big model (i.e. number of keys)
   * @param default The default value to store
   * @param client The glint client
   * @param partitioner A function that creates a partitioner based on a list of models
   * @param props A function that creates an Akka Props object to construct partial models remotely
   * @tparam K The key type to store
   * @tparam V The value type to store
   * @return A BigModel capable of referring to the machines storing the data
   */
  def create[K : ClassTag, V : ClassTag](size: Long,
                                         default: V,
                                         client: Client,
                                         partitioner: (Array[ActorRef]) => Partitioner[K, ActorRef],
                                         props: (Long, Long, V) => Props) : BigModel[K, V] = {

    // Get list of servers from the master
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(30 seconds)
    val servers = Await.result(ask(client.master, new ServerList()), timeout.duration).asInstanceOf[Array[ActorRef]]

    // Spawn partial models on each of the servers
    val models = servers.map(x => x.path.address).zipWithIndex.map {
      case (address, index) =>
        val start = Math.floor(size.toDouble * (index.toDouble / servers.length.toDouble)).toLong
        val end = Math.floor(size.toDouble * ((index.toDouble + 1) / servers.length.toDouble)).toLong
        client.system.actorOf(props(start, end, default).withDeploy(Deploy(scope = RemoteScope(address))))
    }

    // Construct a BigModel object with the appropriate partitioner
    new BigModel[K, V](partitioner(models), default)
  }

}
