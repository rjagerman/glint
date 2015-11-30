package glint.indexing

/**
  * An indexer that uses a cyclic model where keys would be distributed n mod m
  */
class CyclicIndexer(models: Int, keys: Long) extends Indexer[Long] {
  override def index(key: Long): Long = {
    val model = key % models
    val indexInModel = (key - model) / models

    // Due to the cyclic nature of the keys, the first few models will obtain more keys than the rest
    // To model this accurately we first compute how many bigger models there will be
    // Then we compute the bigger model size and the smaller model size (these should always differ by 1)
    val biggerModels = keys % models
    val biggerModelSize = Math.ceil(keys.toDouble / models).toLong
    val smallerModelSize = Math.floor(keys.toDouble / models).toLong

    if (model < biggerModels) {
      // If our selected model is in the first set of models (the bigger one), we use the bigger model size to compute
      // its offset
      indexInModel + model * biggerModelSize
    } else {
      // Otherwise, it is in the later set of models and we need to add all the bigger size models and only a few
      // smaller models to it to find the correct offset
      indexInModel + (model - biggerModels) * smallerModelSize + (biggerModels * biggerModelSize)
    }
  }
}
