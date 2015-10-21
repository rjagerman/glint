/*
package glint.models.array


import akka.actor._
import akka.remote.RemoteScope
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import glint.messages.server.{Push, Response, Pull}
import glint.Client
import glint.messages.master.ServerList
import glint.models.BigModel
import glint.partitioning.{Partitioner, UniformPartitioner}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag


object ArrayBigModel extends StrictLogging {

  /**
   * Creates a big model
   *
   * @param size The size of the model key space
   * @param default The default value to initialize with
   * @param client The client interface
   * @tparam V The type of the values to store
   * @return An array big model instance
   */
  def create[V : ClassTag](size: Long, default: V, client: Client) : BigModel[Long, V] = {

    // Construct "AddressList" message and send to master
    val message = new ServerList()

    // Master response with the list of servers
    implicit val ec = ExecutionContext.Implicits.global
    implicit val timeout = Timeout(10 seconds)
    val servers = Await.result(ask(client.master, message), timeout.duration).asInstanceOf[Array[ActorRef]]

    // Spawn partial models on each of the servers
    val models = servers.map(x => x.path.address).zipWithIndex.map {
      case (address, index) =>
        val start = Math.floor(size.toDouble * (index.toDouble / servers.length.toDouble)).toLong
        val end = Math.floor(size.toDouble * ((index.toDouble + 1) / servers.length.toDouble)).toLong

        logger.info(s"Starting ArrayPartialModel at ${address} for range (${start}, ${end})")
        val prop = Props(classOf[ArrayPartialModel[V]], start, end, default, implicitly[ClassTag[V]])
        client.system.actorOf(prop.withDeploy(Deploy(scope = RemoteScope(address))))
    }

    // Construct a corresponding partitioner
    val partitioner = new UniformPartitioner[ActorRef](models, size)

    // Construct an ArrayBigModel object with the appropriate partitioner
    new BigModel[Long, V](partitioner, default)

  }

}
*/
