package glint.models.server

import akka.actor.{Actor, ActorLogging}
import breeze.linalg.Vector
import breeze.math.Semiring

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * A partial model representing a part of some vector
  *
  * @param start The start index
  * @param end The end index
  * @tparam V The type of value to store
  */
abstract class PartialVector[@specialized V: Semiring : ClassTag](val start: Long,
                                                                  val end: Long) extends Actor with ActorLogging {

  log.info(s"Constructing PartialVector[${implicitly[ClassTag[V]]}] for keys [$start, $end)")

  /**
    * The size of this partial vector
    */
  val size: Int = (end - start).toInt

  /**
    * The data matrix containing the elements
    */
  val data: Vector[V]

  /**
    * Obtains the local integer index of a given global key
    *
    * @param key The global key
    * @return The local index in the data array
    */
  def index(key: Long): Int = (key - start).toInt

  /**
    * Updates the data of this partial model by aggregating given keys and values into it
    *
    * @param keys The keys
    * @param values The values
    */
  def update(keys: Array[Long], values: Array[V]): Boolean = {
    var i = 0
    while (i < keys.length) {
      data.update(index(keys(i)), aggregate(data(index(keys(i))), values(i)))
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
    val ab = new ArrayBuffer[V](keys.length)
    while (i < keys.length) {
      ab += data(index(keys(i)))
      i += 1
    }
    ab.toArray
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
