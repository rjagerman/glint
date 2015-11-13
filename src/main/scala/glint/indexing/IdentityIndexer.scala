package glint.indexing

/**
  * An indexer that just returns the original key
  */
class IdentityIndexer extends Indexer[Long] {
  override def index(key: Long): Long = key
}
