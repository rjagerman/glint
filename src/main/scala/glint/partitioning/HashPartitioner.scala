package glint.partitioning

/**
 * A hash partitioner that uses a DHT-like approach by placing partition hashes into a ring and placing key hashes
 * on the same ring
 *
 * @param partitions An array of possible partitions
 * @param keys The number of keys
 */
class HashPartitioner[P, V](val partitions: Array[P], val keys: Long) extends Partitioner[V, P] {

  val partitionHashes = partitions.map(_.hashCode).zipWithIndex.sortBy(_._1).reverse

  override def partition(key: V): P = {
    val hash = key.hashCode
    partitionHashes.foreach {
      case x if hash > x._1 => return partitions(x._2)
    }
    partitions(partitionHashes(0)._2)
  }
}
