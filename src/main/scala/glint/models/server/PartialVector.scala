package glint.models.server

import akka.actor.{Actor, ActorLogging}
import glint.partitioning.Partition
import glint.utils.HadoopUtils._
import spire.algebra.Semiring
import spire.implicits._

import scala.reflect.ClassTag

/**
  * A partial model representing a part of some vector
  *
  * @param partition The partition
  * @tparam V The type of value to store
  */
private[glint] abstract class PartialVector[@specialized V: Semiring : ClassTag](partition: Partition) extends Actor
  with ActorLogging with PushLogic {

  /**
    * The size of this partial vector
    */
  val size: Int = partition.size

  /**
    * The data matrix containing the elements
    */
  val data: Array[V]

  /**
    * Updates the data of this partial model by aggregating given keys and values into it
    *
    * @param keys The keys
    * @param values The values
    */
  def update(keys: Array[Long], values: Array[V]): Boolean = {
    var i = 0
    while (i < keys.length) {
      val key = partition.globalToLocal(keys(i))
      data(key) += values(i)
      i += 1
    }
    true
  }

  /**
    * Gets the data of this partial model
    *
    * @param keys The keys
    * @return The values
    */
  def get(keys: Array[Long]): Array[V] = {
    var i = 0
    val a = new Array[V](keys.length)
    while (i < keys.length) {
      val key = partition.globalToLocal(keys(i))
      a(i) = data(key)
      i += 1
    }
    a
  }

  /**
    * Write Vector into HDFS specific path with username
    *
    * @param path HDFS path
    * @param user Specific user
    * @return
    */
  def write(path: String, user: String): Boolean = {
    writeHDFS(getStream(path, user, partition.index)) { printer =>
      var count = 0
      (0 until size) foreach { i =>
        if (data(i) != 0.0) {
          count += 1
          printer.write(s"${partition.localToGlobal(i)}:${data(i)}\n")
        }
        if (count > partition.flushNumber()) {
          printer.flush()
          count = 0
        }
      }
      printer.flush()
      true
    }
  }

  log.info(s"Constructed PartialVector[${implicitly[ClassTag[V]]}] of size $size (partition id: ${partition.index})")

}
