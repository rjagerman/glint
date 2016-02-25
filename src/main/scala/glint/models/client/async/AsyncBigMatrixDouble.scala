package glint.models.client.async

import akka.actor.ActorRef
import breeze.linalg.{DenseVector, Vector}
import com.typesafe.config.Config
import glint.indexing.Indexer
import glint.messages.server.request.PushMatrixDouble
import glint.messages.server.response.ResponseDouble
import glint.partitioning.Partitioner
import spire.implicits.cfor

/**
  * Asynchronous implementation of a BigMatrix for doubles
  */
private[glint] class AsyncBigMatrixDouble(partitioner: Partitioner[ActorRef],
                                          indexer: Indexer[Long],
                                          config: Config,
                                          rows: Long,
                                          cols: Int)
  extends AsyncBigMatrix[Double, ResponseDouble, PushMatrixDouble](partitioner, indexer, config, rows, cols) {

  /**
    * Converts the values in given response starting at index start to index end to a vector
    *
    * @param response The response containing the values
    * @param start The start index
    * @param end The end index
    * @return A vector for the range [start, end)
    */
  @inline
  override protected def toVector(response: ResponseDouble, start: Int, end: Int): Vector[Double] = {
    val result = DenseVector.zeros[Double](end - start)
    cfor(0)(_ < end - start, _ + 1)(i => {
      result(i) = response.values(start + i)
    })
    result
  }

  /**
    * Creates a push message from given sequence of rows, columns and values
    *
    * @param rows The rows
    * @param cols The columns
    * @param values The values
    * @return A PushMatrix message for type V
    */
  @inline
  override protected def toPushMessage(rows: Array[Long], cols: Array[Int], values: Array[Double]): PushMatrixDouble = {
    PushMatrixDouble(rows, cols, values)
  }

  /**
    * Extracts a value from a given response at given index
    *
    * @param response The response
    * @param index The index
    * @return The value
    */
  @inline
  override protected def toValue(response: ResponseDouble, index: Int): Double = {
    response.values(index)
  }

}