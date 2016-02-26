package glint.partitioning

import glint.partitioning.cyclic.CyclicPartitioner
import glint.partitioning.range.RangePartitioner
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
  * Partitioning test specification
  */
class PartitioningSpec extends FlatSpec {

  /**
    * Function that asserts whether given key is only in the assigned partition
    *
    * @param key The key
    * @param partitions All the partitions
    * @param assigned The assigned partition
    */
  private def assertKeyInCorrectPartition(key: Long, partitions: Array[Partition], assigned: Partition): Unit = {
    var i = 0
    while (i < partitions.length) {
      if (partitions(i) == assigned) {
        assert(partitions(i).contains(key))
      } else {
        assert(!partitions(i).contains(key))
      }
      i += 1
    }
  }

  "A CyclicPartitioner" should "partition all its keys to partitions that contain it" in {
    val partitioner = CyclicPartitioner(5, 37)
    for (key <- 0 until 37) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "partition an uneven distribution" in {
    val partitioner = CyclicPartitioner(20, 50)
    for (key <- 0 until 50) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "succeed with the same number of partitions and keys" in {
    val partitioner = CyclicPartitioner(13, 13)
    for (key <- 0 until 13) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "succeed with more partitions than keys" in {
    val partitioner = CyclicPartitioner(33, 12)
    for (key <- 0 until 12) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "guarantee unique places within each partition" in {
    val partitioner = CyclicPartitioner(17, 137)
    val partitions = partitioner.all()
    for (partition <- partitions) {
      val data = Array.fill[Boolean](173)(false)
      for (key <- 0 until 137) {
        if (partition.contains(key)) {
          val index = partition.globalToLocal(key)
          assert(!data(index))
          data(index) = true
        }
      }
    }
  }

  it should "fail when attempting to partition keys outside its key space" in {
    val partitioner = CyclicPartitioner(13, 105)
    an[IndexOutOfBoundsException] should be thrownBy partitioner.partition(105)
    an[IndexOutOfBoundsException] should be thrownBy partitioner.partition(-2)
  }

  "A RangePartitioner" should "partition all its keys into partitions that contain it" in {
    val partitioner = RangePartitioner(5, 109)
    for (key <- 0 until 109) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "partition an uneven distribution" in {
    val partitioner = RangePartitioner(20, 50)
    for (key <- 0 until 50) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "succeed with the same number of partitions and keys" in {
    val partitioner = RangePartitioner(13, 13)
    for (key <- 0 until 13) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "succeed with more partitions than keys" in {
    val partitioner = RangePartitioner(33, 12)
    for (key <- 0 until 12) {
      val partition = partitioner.partition(key)
      assertKeyInCorrectPartition(key, partitioner.all(), partition)
    }
  }

  it should "guarantee unique places within each partition" in {
    val partitioner = RangePartitioner(15, 97)
    val partitions = partitioner.all()
    for (partition <- partitions) {
      val data = Array.fill[Boolean](97)(false)
      for (key <- 0 until 97) {
        if (partition.contains(key)) {
          val index = partition.globalToLocal(key)
          assert(!data(index))
          data(index) = true
        }
      }
    }
  }

  it should "fail when attempting to partition keys outside its key space" in {
    val partitioner = RangePartitioner(12, 105)
    an[IndexOutOfBoundsException] should be thrownBy partitioner.partition(105)
    an[IndexOutOfBoundsException] should be thrownBy partitioner.partition(-2)
  }

}