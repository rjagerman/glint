package glint.indexing

/**
  * An indexer that returns the original key
  */
class IdentityIndexer extends Indexer[Long] {

  /**
    * Transforms given key to a long value indicating its index
    *
    * @param key The key
    * @return The index
    */
  override def index(key: Long): Long = key
}
