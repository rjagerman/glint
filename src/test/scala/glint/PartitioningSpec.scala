package glint

import glint.partitioning.UniformPartitioner
import org.scalatest.Matchers._
import org.scalatest.FlatSpec

/**
  * Client test specification
  */
class PartitioningSpec extends FlatSpec {

  "A UniformPartitioner" should " partition all its keys within its partition space" in {
    val partitions = Set(0, 1, 2)
    val up = new UniformPartitioner(partitions.toArray, 19)
    for (key <- 0 until 19) {
      assert(partitions.contains(up.partition(key)))
    }
  }

  it should " fail when partitioning outside its key size" in {
    val up = new UniformPartitioner(Array(0, 1, 2, 3, 4), 11)
    an [IndexOutOfBoundsException] should be thrownBy up.partition(11)
    an [IndexOutOfBoundsException] should be thrownBy up.partition(-2)
  }

  it should " succeed when creating a partitioner with exactly the same amount of keys and partitions" in {
    val up = new UniformPartitioner(Array(0, 1, 2, 3), 4)
  }

}