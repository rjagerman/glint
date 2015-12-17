package glint.partitioning

/**
  * A uniform key partitioner with fixed number of servers and keys
  *
  * @param partitionsArray An array of possible partitions
  * @param keys The number of keys
  */
class UniformPartitioner[P](partitionsArray: Array[P], val keys: Long) extends Partitioner[P] {
  assert(keys >= partitionsArray.length, "cannot create a partitioner with less keys than partitions")

  override def partition(key: Long): P = {
    partitionsArray(Math.floor((key.toDouble / keys.toDouble) * partitionsArray.length.toDouble).toInt)
  }

  override def partitions: Seq[P] = partitionsArray.toSeq

  override def start(partition: P): Long = start(partitionsArray.indexOf(partition))

  override def end(partition: P): Long = end(partitionsArray.indexOf(partition))

  /**
    * Computes the start index of partition p
    *
    * @param p A long representing the index of partition p
    * @return The start index
    */
  private def start(p: Long): Long = {
    Math.ceil(p * (keys.toDouble / partitionsArray.length.toDouble)).toLong
  }

  /**
    * Computes the end index of partition p
    *
    * @param p A long representing the index of partition p
    * @return The end index
    */
  private def end(p: Long): Long = {
    start(p + 1)
  }
}

