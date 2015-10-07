package glint.partitioning

/**
 * Partitioners allocate a server id (int) for each key (long)
 */
trait Partitioner {

  /**
   * Assign a server ID to the given key
   *
   * @param key The key to partition
   * @return The server this key belongs to
   */
  def partition(key: Long): Int
}
