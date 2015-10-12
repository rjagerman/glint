package glint.partitioning

/**
 * Partitioners allocate a server id for each key
 */
trait Partitioner[K, P] {

  /**
   * Assign a server to the given key
   *
   * @param key The key to partition
   * @return The partition
   */
  def partition(key: K): P

}
