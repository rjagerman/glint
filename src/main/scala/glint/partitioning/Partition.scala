package glint.partitioning

/**
  * An abstract partition
  *
  * @param index The index of this partition
  */
abstract class Partition(val index: Int) extends Serializable {

  /**
    * Checks whether given global key falls within this partition
    *
    * @param key The key
    * @return True if the global key falls within this partition, false otherwise
    */
  @inline
  def contains(key: Long): Boolean

  /**
    * Converts given global key to a continuous local array index [0, 1, ...]
    *
    * @param key The global key
    * @return The local index
    */
  @inline
  def globalToLocal(key: Long): Int

  /**
    * Computes the size of this partition
    *
    * @return The size of this partition
    */
  def size: Int

}
