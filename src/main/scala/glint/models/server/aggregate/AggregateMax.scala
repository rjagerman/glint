package glint.models.server.aggregate

import spire.algebra.{Order, Semiring}
import spire.implicits._
import scala.reflect.ClassTag

/**
  * Aggregation through maximum
  */
class AggregateMax extends Aggregate {

  /**
    * Takes the maximum of two values
    *
    * @param v1 The first value
    * @param v2 The second value
    * @tparam V The type of the parameters
    * @return The resulting value
    */
  @inline
  override def aggregate[@specialized V: Semiring : Order : ClassTag](v1: V, v2: V): V = v1.max(v2)

}

object AggregateMax {
  def apply(): AggregateMax = new AggregateMax()
}