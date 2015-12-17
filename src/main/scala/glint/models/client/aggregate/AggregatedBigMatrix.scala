package glint.models.client.aggregate

import akka.util.Timeout
import breeze.linalg.Vector
import glint.models.client.BigMatrix

import scala.collection.mutable.HashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A big matrix implementation that adds functionality to aggregate push requests together
  */
class AggregatedBigMatrix[V: ClassTag](underlying: BigMatrix[V],
                                       aggregate: (V, V) => V,
                                       default: V) extends BigMatrix[V] {

  val map = HashMap.empty[(Long, Int), V]

  /**
    * Pulls a set of rows
    *
    * @param rows The indices of the rows
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[Vector[V]]] = {
    underlying.pull(rows)
  }

  /**
    * Pushes a set of values
    *
    * @param rows The indices of the rows
    * @param cols The indices of the columns
    * @param values The values to update
    * @return A future containing either the success or failure of the operation
    */
  override def push(rows: Array[Long],
                    cols: Array[Int],
                    values: Array[V])(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    underlying.push(rows, cols, values)
  }

  /**
    * Pulls a set of elements
    *
    * @param rows The indices of the rows
    * @param cols The corresponding indices of the columns
    * @return A future containing the values of the elements at given rows, columns
    */
  override def pull(rows: Array[Long],
                    cols: Array[Int])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[V]] = {
    underlying.pull(rows, cols)
  }

  /**
    * Destroys the big matrix
    *
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = {
    underlying.destroy()
  }

  /**
    * Pushes a new value into the local storage aggregating with previous values when necessary
    * This does not actually push anything to the parameter servers until you call `flush()`
    *
    * @param row The row
    * @param col The column
    * @param value The value
    */
  def aggregatePush(row: Long, col: Int, value: V): Unit = {
    map.put((row, col), aggregate(map.getOrElse((row, col), default), value))
  }

  /**
    * @return The size of the local aggregated storage
    */
  def localSize(): Int = map.size

  /**
    * Flushes the current local aggregated storage to the parameter servers and clears the local storage
    *
    * @return A future indicating the success or failure of pushing the values to the parameter servers
    */
  def flush()(implicit ec: ExecutionContext, timeout: Timeout): Future[Boolean] = {
    val rows = new Array[Long](map.size)
    val cols = new Array[Int](map.size)
    val values = new Array[V](map.size)
    var i = 0
    val iterator = map.keysIterator
    while (i < map.size) {
      val (row, col) = iterator.next()
      rows(i) = row
      cols(i) = col
      values(i) = map((row, col))
      i += 1
    }
    map.clear()
    push(rows, cols, values)
  }

}
