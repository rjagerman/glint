package glint.models.server

import akka.actor.{Actor, ActorLogging}
import glint.partitioning.Partition
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
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

  val numberOfFlush: Int = 1000
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
    * Save data to HDFS
    *
    * @param path HDFS path, the full path should be hdfs://path/part-${parition.idex + 1}
    * @return Boolean store OK
    */
  def save(path: String, conf: Configuration): Boolean = {
    val name = path + "/part-" + partition.index
    val fds = FileSystem
      .get(conf)
      .create(new Path(name))

    writeToFile(fds) { printer =>
      var i = 0
      (data.indices zip data)
        .filter { case (index, value: V) => value != 0.0 }
        .map { case (index: Int, value) => (partition.localToGlobal(index), value) }
        .foreach { case (key: Long, value) =>
          printer.println(s"$key:$value")
          if (i > numberOfFlush) {
            i = 0
            printer.flush()
          }
          i += 1
        }
    }
  }

  log.info(s"Constructed PartialVector[${implicitly[ClassTag[V]]}] of size $size (partition id: ${partition.index})")

}
