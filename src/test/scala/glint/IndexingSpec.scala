package glint

import glint.indexing.CyclicIndexer
import org.scalatest.FlatSpec

/**
 * Test specification for indexers
 */
class IndexingSpec extends FlatSpec {

  "An CyclicIndexer" should " index all unique keys into unique new keys" in {

    val features = 20655
    val models = 21
    var outputSet = Set.empty[Long]

    val ci = new CyclicIndexer(models, features)
    for (i <- 0 until features) {
      val o = ci.index(i)
      assert(!outputSet.contains(o))
      outputSet += o
    }

  }
}
