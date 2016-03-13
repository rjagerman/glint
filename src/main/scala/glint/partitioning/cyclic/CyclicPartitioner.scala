package glint.partitioning.cyclic

import glint.partitioning.{Partition, Partitioner}

/**
  * A cyclic key partitioner
  *
  * @param partitions The partitions of this cyclic partitioner
  */
class CyclicPartitioner(partitions: Array[Partition], keys: Long) extends Partitioner {

  /**
    * Assign a server to the given key according to a cyclic modulo partitioning scheme
    *
    * @param key The key to partition
    * @return The partition
    */
  @inline
  override def partition(key: Long): Partition = {
    if (key >= keys) { throw new IndexOutOfBoundsException() }
    partitions((key % partitions.length).toInt)
  }

  /**
    * Returns all partitions
    *
    * @return The array of partitions
    */
  override def all(): Array[Partition] = partitions
}

object CyclicPartitioner {

  /**
    * Creates a CyclicPartitioner for given number of partitions and keys
    *
    * @param numberOfPartitions The number of partitions
    * @param numberOfKeys The number of keys
    * @return A CyclicPartitioner
    */
  def apply(numberOfPartitions: Int, numberOfKeys: Long): CyclicPartitioner = {
    val partitions = new Array[Partition](numberOfPartitions)
    var i = 0
    while (i < numberOfPartitions) {
      partitions(i) = new CyclicPartition(i, numberOfPartitions, numberOfKeys)
      i += 1
    }
    new CyclicPartitioner(partitions, numberOfKeys)
  }

}