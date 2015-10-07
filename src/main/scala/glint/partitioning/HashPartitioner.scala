package glint.partitioning

/**
 * A hash key partitioner with fixed number of servers
 *
 * @param servers The number of servers
 */
class HashPartitioner(servers: Int) extends Partitioner {
  override def partition(key: Long): Int = (Math.abs(scala.util.hashing.byteswap64(key)) % servers).toInt
}
