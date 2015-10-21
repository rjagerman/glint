package glint

import java.io.File

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.models.BigModel

import scala.reflect.ClassTag

/**
 * A client interface that facilitates easy communication with the master and provides easy-to-use functions to spawn
 * large models on the parameter servers.
 *
 * A typical usage scenario (pull/push a distributed dense array with 10000 values initialized to 0.0):
 *
 * {{{
 *   val client = new Client()
 *   val bigModel = client.dense[Double](10000, 0.0)
 *
 *   bigModel.pull(Array(1, 2, 5000, 8000)).onSuccess { case values => println(values.mkString(", ")) }
 *   bigModel.push(Array(1, 2, 300, 40), Array(0.1, 0.2, 300.2, 0.001))
 * }}}
 *
 * @constructor Create a new client with optional configuration (see default.conf for an example)
 * @param config The configuration
 */
class Client(config: Config = ConfigFactory.parseFile(new File(getClass.getClassLoader.getResource("default.conf").getFile))) extends StrictLogging {

  private val akkaConfig = ConfigFactory.parseString(
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
            hostname = ${config.getString("glint.client.host")}
            port = ${config.getInt("glint.client.port")}
          }
        }
      }
      """.stripMargin)

  private val masterHost = config.getString("glint.master.host")
  private val masterPort = config.getInt("glint.master.port")
  private val masterName = config.getString("glint.master.name")
  private val masterSystem = config.getString("glint.master.system")

  @transient
  private[glint] lazy val system = ActorSystem(config.getString("glint.client.system"), akkaConfig)

  @transient
  private[glint] lazy val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")

  /**
   * Constructs a distributed dense array (indexed by Long) over the parameter servers
   * The returned model is serializable and safe to be used across machines (e.g. in Spark workers)
   *
   * Typical usage:
   * {{{
   *   val model = client.dense[Double](10000, 0.0)
   * }}}
   *
   * @param size The total size
   * @param default The default value to populate the array with
   * @tparam V The type of values to store
   * @return A reference BigModel
   */
  def dense[V : ClassTag](size: Long, default: V): BigModel[Long, V] = models.array[V](size, default, this)

}
