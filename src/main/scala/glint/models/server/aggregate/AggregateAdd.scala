package glint.models.server.aggregate

import spire.algebra.{Order, Semiring}
import spire.implicits._
import scala.reflect.ClassTag

/**
  * Aggregation through addition
  */
class AggregateAdd extends Aggregate {

  /**
    * Adds two values of type V together
    *
    * @param v1 The first value
    * @param v2 The second value
    * @tparam V The type of the parameters
    * @return The resulting value
    */
  @inline
  override def aggregate[@specialized V: Semiring : Order : ClassTag](v1: V, v2: V): V = v1 + v2

}

object AggregateAdd {
  def apply(): AggregateAdd = new AggregateAdd()
}