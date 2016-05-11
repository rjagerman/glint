package glint.models.server.aggregate

import spire.algebra.{Order, Semiring}
import spire.implicits._
import scala.reflect.ClassTag

/**
  * Aggregation through minimum
  */
class AggregateMin extends Aggregate {

  /**
    * Takes the minimum of the two values
    *
    * @param v1 The first value
    * @param v2 The second value
    * @tparam V The type of the parameters
    * @return The resulting value
    */
  @inline
  override def aggregate[@specialized V: Semiring : Order : ClassTag](v1: V, v2: V): V = v1.min(v2)

}

object AggregateMin {
  def apply(): AggregateMin = new AggregateMin()
}
