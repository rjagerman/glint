package glint.models.contiguous

import glint.models.PartialModel

import scala.reflect.ClassTag

/**
 * A model that has a contiguous key space per partial model. The underlying representation of data is a large array
 * with values of type V.
 */
class ContiguousPartialModel[K, V : ClassTag, D](start: Long,
                            end: Long,
                            construct: => V,
                            index: K => Long,
                            push: (V, D) => V) extends PartialModel[K, V, D] {

  /**
   * The local partial array of data
   *
   * Since our global key space is potentially very large we globally use a Long as an indexing type. The partial array
   * here can only be indexed through an integer format due to limitations of the JVM. We convert our global index to a
   * local index by subtracting this partial model's start index. We assume that each partial model contains at most
   * Int.MaxValue (roughly 2 billion) elements.
   */
  val data = Array.fill[V]((end - start).toInt)(construct)

  /**
   * Converts given key to a local index in the data array by subtracting this partial model's global start index
   *
   * @param key The key
   * @return The index in the data array
   */
  private def toLocalIndex(key: K): Int = (index(key) - start).toInt

  override def getValue(key: K): V = data(toLocalIndex(key))

  override def setValue(key: K, value: D): Unit = {
    val old = getValue(key)
    data(toLocalIndex(key)) = push(old, value)
  }

}
