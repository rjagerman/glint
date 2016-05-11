package glint.models.server.aggregate

import spire.algebra.{Order, Semiring}

import scala.reflect.ClassTag

/**
  * Basic aggregate trait
  */
trait Aggregate extends Serializable {

  @inline
  def aggregate[@specialized V: Semiring : Order : ClassTag](v1: V, v2: V): V

}
