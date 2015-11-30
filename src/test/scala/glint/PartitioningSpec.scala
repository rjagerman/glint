package glint

import glint.partitioning.UniformPartitioner
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
  * Partitioning test specification
  */
class PartitioningSpec extends FlatSpec {

  "A UniformPartitioner" should " partition all its keys within its partition space" in {
    val partitions = Set(0, 1, 2)
    val up = new UniformPartitioner(partitions.toArray, 19)
    for (key <- 0 until 19) {
      assert(partitions.contains(up.partition(key)))
    }
  }

  it should " succeed with an uneven distribution" in {
    val partitions = (0 until 20).toArray
    val keys = (0 until 50).toArray
    val partitioner = new UniformPartitioner(partitions, keys.length)
    for (key <- keys) {
      val p = partitioner.partition(key)
      assert(partitioner.start(p) <= key)
      assert(partitioner.end(p) > key)
    }
  }

  it should " succeed with an even number of partitions and keys" in {
    val partitions = Array(0, 1, 2)
    val keys = Array(0, 1, 2)
    val partitioner = new UniformPartitioner(partitions, keys.length)
    for (key <- keys) {
      assert(partitioner.partition(key) == key)
    }
  }

  it should " succeed when creating a partitioner with exactly the same amount of keys and partitions" in {
    val up = new UniformPartitioner(Array(0, 1, 2, 3), 4)
  }

  it should " fail when partitioning outside its key size" in {
    val up = new UniformPartitioner(Array(0, 1, 2, 3, 4), 11)
    an [IndexOutOfBoundsException] should be thrownBy up.partition(11)
    an [IndexOutOfBoundsException] should be thrownBy up.partition(-2)
  }

}