package glint.models.server

import breeze.linalg._
import breeze.math._

import scala.reflect.ClassTag

/**
  * Implementation of the ArrayPartialModel using stored Scalar values and addition as an update mechanic
  *
  * @param start The start index
  * @param end The end index
  * @param default The default value
  * @tparam V The type of values to store (must be of breeze vector type, e.g. DenseVector[Int], etc.)
  */
class VectorArrayPartialModel[V: Semiring : ClassTag](start: Long,
                                                      end: Long,
                                                      default: Vector[V]) extends ArrayPartialModel[Vector[V]](start, end, default) {

  def update(key: Long, value: Vector[V]): Unit = {
    data(index(key)) = data(index(key)) :+ value
  }

}
