package glint.indexing

/**
  * An indexer converting a key K to a Long so it can be used by a partitioner
  */
trait Indexer[K] extends Serializable {

  /**
    * Transforms given key to a long value indicating its index
    *
    * @param key The key
    * @return The index
    */
  def index(key: K): Long

}
