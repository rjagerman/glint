package glint.models.client.granular

import akka.util.Timeout
import breeze.linalg.Vector
import glint.models.client.BigMatrix

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, ExecutionContext}
import scala.reflect.ClassTag

/**
  * A BigMatrix whose messages (even for large pulls/pushes) are granular and limited to a specific maximum message
  * size. This helps resolve timeout exceptions and heartbeat failures in akka at the cost of additional message
  * overhead.
  *
  * @param underlying The underlying big matrix
  * @param cols The number of columns in the big matrix
  * @param maximumMessageSize The maximum message size
  * @tparam V The type of values to store
  */
class GranularBigMatrix[V: ClassTag](underlying: BigMatrix[V],
                                     cols: Int,
                                     maximumMessageSize: Int) extends BigMatrix[V] {

  /**
    * Pulls a set of rows
    *
    * @param rows The indices of the rows
    * @return A future containing the vectors representing the rows
    */
  override def pull(rows: Array[Long])(implicit timeout: Timeout, ec: ExecutionContext): Future[Array[Vector[V]]] = {
    var i = 0
    val maxIncrement = Math.max(1, maximumMessageSize / cols)
    val ab = new ArrayBuffer[Future[Array[Vector[V]]]](rows.length / maxIncrement)
    while (i < rows.length) {
      val end = Math.min(rows.length, i + maxIncrement)
      val future = underlying.pull(rows.slice(i, end))
      ab.append(future)
      i += maxIncrement
    }
    Future.sequence(ab.toIterator).map {
      case arrayOfValues =>
        val finalValues = new ArrayBuffer[Vector[V]](rows.length)
        arrayOfValues.foreach(x => finalValues.appendAll(x))
        finalValues.toArray
    }
  }

  /**
    * Destroys the big matrix and its resources on the parameter server
    *
    * @return A future whether the matrix was successfully destroyed
    */
  override def destroy()(implicit timeout: Timeout, ec: ExecutionContext): Future[Boolean] = underlying.destroy()

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
    var i = 0
    val ab = new ArrayBuffer[Future[Boolean]](rows.length / maximumMessageSize)
    while (i < rows.length) {
      val end = Math.min(rows.length, i + maximumMessageSize)
      val future = underlying.push(rows.slice(i, end), cols.slice(i, end), values.slice(i, end))
      ab.append(future)
      i += maximumMessageSize
    }
    Future.sequence(ab.toIterator).transform(x => x.forall(y => y), err => err)
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
    var i = 0
    val ab = new ArrayBuffer[Future[Array[V]]](rows.length / maximumMessageSize)
    while (i < rows.length) {
      val end = Math.min(rows.length, i + maximumMessageSize)
      val future = underlying.pull(rows.slice(i, end), cols.slice(i, end))
      ab.append(future)
      i += maximumMessageSize
    }
    Future.sequence(ab.toIterator).map {
      case arrayOfValues =>
        val finalValues = new ArrayBuffer[V](rows.length)
        arrayOfValues.foreach(x => finalValues.appendAll(x))
        finalValues.toArray
    }
  }
}
