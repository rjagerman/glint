package glint.models.array

import akka.actor._
import akka.remote.RemoteScope
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import glint.messages.server.{Push, Response, Pull}
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
                                  val partitioner: Partitioner[Long, ActorRef])
  extends BigModel[Long, V] with StrictLogging {

  /**
   * Asynchronous function to pull values from the big model
   *
   * @param keys The array of keys
   * @return A future for the array of returned values
   */
  override def pull(keys: Array[Long]): Future[Array[V]] = {
    implicit val ec = ExecutionContext.Implicits.global

    // Send pull request of the list of keys
    val pulls = keys.groupBy(partitioner.partition).map {
      case (partition, partitionKeys) =>
        ask(partition, Pull(partitionKeys))(Timeout(30 seconds)).mapTo[Response[V]]
    }

    // Obtain key indices after partitioning so we can place the results in a correctly ordered array
    val indices = keys.zipWithIndex.groupBy {
      case (k, i) => partitioner.partition(k)
    }.map {
      case (_, arr) => arr.map(_._2)
    }

    // Define aggregator for successfull responses
    def aggregateSuccess(responses: Iterable[Response[V]]): Array[V] = {
      val result = Array.fill[V](keys.length)(default)
      responses.zip(indices).foreach {
        case (response, idx) => idx.zip(response.values).foreach {
          case (k, v) => result(k) = v
        }
      }
      result
    }

    // Combine and aggregate futures
    Future.sequence(pulls).transform(aggregateSuccess, err => err)

  }

  /**
   * Asynchronous function to push values to the big model
   *
   * @param keys The array of keys
   * @param values The array of values
   * @return A future for the completion of the operation
   */
  override def push(keys: Array[Long], values: Array[V]): Future[Unit] = {
    implicit val ec = ExecutionContext.Implicits.global

    // Send push request
    val pushes = keys.zip(values).groupBy{
      case (k,v) => partitioner.partition(k)
    }.map {
      case (partition, keyValuePairs) =>
        ask(partition, Push(keyValuePairs.map(_._1), keyValuePairs.map(_._2)))(Timeout(30 seconds))
    }

    // Combine and aggregate futures
    Future.sequence(pushes).transform(results => Unit, err => err)
  }
}


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
        val start = Math.floor(size.toDouble * (index.toDouble / servers.length.toDouble)).toLong
        val end = Math.floor(size.toDouble * ((index.toDouble + 1) / servers.length.toDouble)).toLong

        logger.info(s"Starting ArrayPartialModel at ${address} for range (${start}, ${end})")
        val prop = Props(classOf[ArrayPartialModel[V]], start, end, default, implicitly[ClassTag[V]])
        client.system.actorOf(prop.withDeploy(Deploy(scope = RemoteScope(address))))
    }

    // Construct a corresponding partitioner
    val partitioner = new UniformPartitioner[ActorRef](models, size)

    // Construct an ArrayBigModel object with the appropriate partitioner
    new ArrayBigModel[V](default, partitioner)

  }

}

