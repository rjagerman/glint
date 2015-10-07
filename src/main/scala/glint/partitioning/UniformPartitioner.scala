package glint.partitioning

/**
 * A uniform key partitioner with fixed number of servers and keys
 *
 * @param servers The number of servers
 * @param keys The number of keys
 */
class UniformPartitioner(servers: Int, keys: Long) extends Partitioner {
  override def partition(key: Long): Int = Math.floor( (key.toDouble / keys.toDouble) * servers.toDouble ).toInt
}
