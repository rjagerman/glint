package glint.models.server

import akka.actor.{Actor, ActorLogging}
import spire.algebra.Semiring
import spire.implicits._
import glint.partitioning.Partition
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

import scala.reflect.ClassTag

/**
  * A partial model representing a part of some matrix
  *
  * @param partition The partition of data this partial matrix represents
  * @param cols The number of columns
  * @tparam V The type of value to store
  */
private[glint] abstract class PartialMatrix[@specialized V: Semiring : ClassTag](val partition: Partition,
                                                                                 val cols: Int) extends Actor
  with ActorLogging with PushLogic {

  /**
    * Number Of data will trigger one flush operation to write data
    */
  val numberOfFlush: Int = 1000

  /**
    * The size of this partial matrix in number of rows
    */
  val rows: Int = partition.size

  /**
    * The data matrix containing the elements
    */
  val data: Array[Array[V]]

  /**
    * Gets rows from the data matrix
    *
    * @param rows The row indices
    * @return A sequence of values
    */
  def getRows(rows: Array[Long]): Array[Array[V]] = {
    var i = 0
    val a = new Array[Array[V]](rows.length)
    while (i < rows.length) {
      val row = partition.globalToLocal(rows(i))
      a(i) = data(row)
      i += 1
    }
    a
  }

  /**
    * Gets values from the data matrix
    *
    * @param rows The row indices
    * @param cols The column indices
    * @return A sequence of values
    */
  def get(rows: Array[Long], cols: Array[Int]): Array[V] = {
    var i = 0
    val a = new Array[V](rows.length)
    while (i < rows.length) {
      val row = partition.globalToLocal(rows(i))
      val col = cols(i)
      a(i) = data(row)(col)
      i += 1
    }
    a
  }

  /**
    * Updates the data of this partial model by aggregating given keys and values into it
    *
    * @param rows The rows
    * @param cols The cols
    * @param values The values
    */
  def update(rows: Array[Long], cols: Array[Int], values: Array[V]): Boolean = {
    var i = 0
    while (i < rows.length) {
      val row = partition.globalToLocal(rows(i))
      val col = cols(i)
      data(row)(col) += values(i)
      i += 1
    }
    true
  }

  /**
    * Save data to HDFS
    *   data schema $row:$col:$value
    *
    * @param path HDFS path, the full path should be hdfs://path/part-${partition.index}
    * @return Boolean store OK
    */
  def save(path: String, conf: Configuration): Boolean = {
    val name = path + "/part-" + partition.index
    val fds = FileSystem.get(conf).create(new Path(name))

    writeToFile(fds) { printer =>
      (0 until rows) foreach { rindex =>
        var i = 0
        val dd = data(rindex)
        val row = partition.localToGlobal(rindex)
        (dd.indices zip dd)
          .filter { case (col, value: V) => value != 0.0 }
          .foreach { case (col, value) =>
              printer.println(s"$row:$col:$value")
              if (i > numberOfFlush) {
                i = 0
                printer.flush()
              }
              i += 1
          }
        printer.flush()
      }
      printer.flush()
    }
  }

  log.info(s"Constructed PartialMatrix[${implicitly[ClassTag[V]]}] with $rows rows and $cols columns (partition id: ${partition.index})")

}
