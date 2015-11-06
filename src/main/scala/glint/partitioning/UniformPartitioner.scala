package glint.partitioning

/**
 * A uniform key partitioner with fixed number of servers and keys
 *
 * @param partitions An array of possible partitions
 * @param keys The number of keys
 */
class UniformPartitioner[P](val partitions: Array[P], val keys: Long) extends Partitioner[Long, P] {
  override def partition(key: Long): P = {
    partitions(Math.floor((key.toDouble / keys.toDouble) * partitions.length.toDouble).toInt)
  }
}

