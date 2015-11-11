package glint.partitioning

/**
  * Partitioners allocate a server id for each key
  */
trait Partitioner[P] extends Serializable {

  /**
    * Assign a server to the given key
    *
    * @param key The key to partition
    * @return The partition
    */
  def partition(key: Long): P

  /**
    * Computes the start key for given partition
    *
    * @param partition The partition
    * @return The start key
    */
  def start(partition: P): Long

  /**
    * Computes the last key for given partition
    * @param partition The partition
    * @return The last key
    */
  def end(partition: P): Long

}
