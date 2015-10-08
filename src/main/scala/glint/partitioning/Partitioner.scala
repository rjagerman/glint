package glint.partitioning

import glint.models.PartialModelRef

/**
 * Partitioners allocate a server id for each key
 */
trait Partitioner[K] {

  /**
   * Assign a server to the given key
   *
   * @param key The key to partition
   * @return Reference to the partial model this key belongs to
   */
  def partition(key: K): PartialModelRef
}
