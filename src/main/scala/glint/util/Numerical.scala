package glint.util

import scala.reflect.runtime.universe.{TypeTag, typeOf}

/**
  * Class for checking accepted numerical types
  *
  * @tparam T The type (either Int, Long, Float or Double)
  */
private[glint] class Numerical[T]

/**
  * Contains the type witnesses that define which types we want to accept
  */
private[glint] object Numerical {
  implicit object IntWitness extends Numerical[Int]
  implicit object LongWitness extends Numerical[Long]
  implicit object FloatWitness extends Numerical[Float]
  implicit object DoubleWitness extends Numerical[Double]

  /**
    * Determines number type at runtime of given type tag
    *
    * @tparam V The type to infer
    * @return The string representation of the type
    */
  def asString[V: TypeTag]: String = {
    implicitly[TypeTag[V]].tpe match {
      case x if x <:< typeOf[Int] => "Int"
      case x if x <:< typeOf[Long] => "Long"
      case x if x <:< typeOf[Float] => "Float"
      case x if x <:< typeOf[Double] => "Double"
      case x => s"${x.toString}"
    }
  }
}
