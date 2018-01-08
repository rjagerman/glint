package glint.partitioning.range

import glint.partitioning.Partition

/**
  * A range partition
  */
class RangePartition(index: Int, val start: Long, val end: Long) extends Partition(index) {

  /**
    * Checks whether given global key falls within this partition
    *
    * @param key The key
    * @return True if the global key falls within this partition, false otherwise
    */
  @inline
  override def contains(key: Long): Boolean = key >= start && key < end

  /**
    * Computes the size of this partition
    *
    * @return The size of this partition
    */
  override def size: Int = (end - start).toInt

  /**
    * Converts given global key to a continuous local array index [0, 1, ...]
    *
    * @param key The global key
    * @return The local index
    */
  @inline
  override def globalToLocal(key: Long): Int = (key - start).toInt

  /**
    * Converts given local index to global key
    *
    * @param index The local index
    * @return The Global key
    */
  @inline
  override def localToGlobal(index: Int): Long = index.toLong + start
}
