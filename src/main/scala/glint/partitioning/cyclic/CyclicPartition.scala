package glint.partitioning.cyclic

import glint.partitioning.Partition

/**
  * A cyclic partitioner
  *
  * @param index              The index of this partition
  * @param numberOfPartitions The total number of partitions
  * @param numberOfKeys       The total number of keys
  */
class CyclicPartition(index: Int, val numberOfPartitions: Int, numberOfKeys: Long) extends Partition(index) {

  /**
    * Checks whether given global key falls within this partition
    *
    * @param key The key
    * @return True if the global key falls within this partition, false otherwise
    */
  @inline
  override def contains(key: Long): Boolean = {
    (key % numberOfPartitions).toInt == index
  }

  /**
    * Computes the size of this partition
    *
    * @return The size of this partition
    */
  override def size: Int = {
    var i = 1
    while (!contains(numberOfKeys - i)) {
      i += 1
    }
    globalToLocal(numberOfKeys - i) + 1
  }

  /**
    * Converts given global key to a continuous local array index [0, 1, ...]
    *
    * @param key The global key
    * @return The local index
    */
  @inline
  override def globalToLocal(key: Long): Int = {
    ((key - index) / numberOfPartitions).toInt
  }

  /**
    * Converts given local index to global key
    *
    * @param index The local index
    * @return The Global key
    */
  @inline
  override def localToGlobal(index: Int): Long = {
    index.toLong * numberOfPartitions + index
  }

}
