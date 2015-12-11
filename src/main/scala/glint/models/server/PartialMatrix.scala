package glint.models.server

import akka.actor.{Actor, ActorLogging}
import breeze.linalg.Matrix
import breeze.math.Semiring

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * A partial model representing a part of some matrix
  *
  * @param start The row start index
  * @param end The row end index
  * @param cols The number of columns
  * @tparam V The type of value to store
  */
abstract class PartialMatrix[@specialized V: Semiring : ClassTag](val start: Long,
                                                                  val end: Long,
                                                                  val cols: Int) extends Actor with ActorLogging {

  log.info(s"Constructing PartialMatrix[${implicitly[ClassTag[V]]}] with $cols columns for rows [$start, $end)")

  /**
    * The size of this partial matrix in number of rows
    */
  val rows = (end - start).toInt

  /**
    * The data matrix containing the elements
    */
  val data: Matrix[V]

  /**
    * Obtains the local integer index of a given global key
    *
    * @param key The global key
    * @return The local index in the data array
    */
  def index(key: Long): Int = (key - start).toInt

  /**
    * Gets rows from the data matrix
    *
    * @param rows The row indices
    * @return A sequence of values
    */
  def getRows(rows: Array[Long]): Array[V] = {
    var i = 0
    val ab = new ArrayBuffer[V](rows.length * cols)
    while (i < rows.length) {
      var j = 0
      while (j < cols) {
        ab += data(index(rows(i)), j)
        j += 1
      }
      i += 1
    }
    ab.toArray
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
    val ab = new ArrayBuffer[V](rows.length)
    while (i < rows.length) {
      ab += data(index(rows(i)), cols(i))
      i += 1
    }
    ab.toArray
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
      data.update(index(rows(i)), cols(i), aggregate(data(index(rows(i)), cols(i)), values(i)))
      i += 1
    }
    true
  }

  /**
    * Aggregates to values of type V together
    *
    * @param value1 The first value
    * @param value2 The second value
    * @return The aggregated value
    */
  def aggregate(value1: V, value2: V): V

}
