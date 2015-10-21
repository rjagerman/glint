package glint

import java.io.File

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import glint.models.BigModel

import scala.reflect.ClassTag

/**
 * A client interface that facilitates easy communication with the master
 *
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
  lazy val system = ActorSystem(config.getString("glint.client.system"), akkaConfig)

  @transient
  lazy val master = system.actorSelection(s"akka.tcp://${masterSystem}@${masterHost}:${masterPort}/user/${masterName}")

  /**
   * Constructs a distributed dense array (indexed by Long) over the parameter servers
   *
   * @param size The total size
   * @param default The default value to populate the array with
   * @tparam V The type of values to store
   * @return A reference BigModel
   */
  def dense[V : ClassTag](size: Long, default: V): BigModel[Long, V] = models.array[V](size, default, this)

}
