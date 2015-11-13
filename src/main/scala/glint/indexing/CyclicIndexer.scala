package glint.indexing

/**
  * An indexer that uses a cyclic model where keys would be distributed n mod m
  */
class CyclicIndexer(models: Int, keys: Long) extends Indexer[Long] {
  override def index(key: Long): Long = {
    Math.ceil((key % models) * (keys / models)).toLong + Math.floor(key / models).toLong
  }
}
