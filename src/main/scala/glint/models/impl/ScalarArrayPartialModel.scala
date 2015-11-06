package glint.models.impl

import spire.algebra._
import spire.implicits._

import scala.reflect.ClassTag

/**
 * Implementation of the ArrayPartialModel using stored Scalar values and addition as an update mechanic
 *
 * @param start The start index
 * @param end The end index
 * @param default The default value
 * @tparam V The type of values to store (must be of algebraic type Semiring, e.g. Double, Int, Long, etc.)
 */
class ScalarArrayPartialModel[V : Semiring : ClassTag](start: Long,
                                                       end: Long,
                                                       default: V) extends ArrayPartialModel[V](start, end, default) {

  def update(key: Long, value: V): Unit = {
    data(index(key)) += value
  }

}
