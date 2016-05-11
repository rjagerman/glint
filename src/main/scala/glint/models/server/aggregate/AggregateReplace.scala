package glint.models.server.aggregate

import spire.algebra.{Order, Semiring}
import spire.implicits._
import scala.reflect.ClassTag

/**
  * Aggregation by replacing the old with the new
  */
class AggregateReplace extends Aggregate {

  /**
    * Aggregates two values of type V together
    *
    * @param v1 The first value
    * @param v2 The second value
    * @tparam V The type of the parameters
    * @return The resulting value
    */
  @inline
  override def aggregate[@specialized V: Semiring : Order : ClassTag](v1: V, v2: V): V = v2

}

object AggregateReplace {
  def apply(): AggregateReplace = new AggregateReplace()
}