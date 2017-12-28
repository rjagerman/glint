package glint.partitioning

import glint.partitioning.cyclic.{CyclicPartition, CyclicPartitioner}
import glint.partitioning.range.{RangePartition, RangePartitioner}
import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import org.scalameter.{Bench, Gen}

import scala.util.Random

/**
  * Benchmarks partitioning
  */
object PartitioningBenchmark extends Bench.OfflineReport {

  // Configuration
  override lazy val executor = LocalExecutor(new Executor.Warmer.Default, aggregator, measurer)
  exec.reinstantiation.frequency -> 4

  // Initialize
  val random = new Random(42)

  // Data for partitioner tests
  val keySize = 109231
  val rangePartitioner = RangePartitioner(90, keySize)
  val cyclicPartitioner = CyclicPartitioner(90, keySize)
  val partitionerSizes = Gen.range("size")(500000, 1000000, 100000)
  val partitionerData = for (size <- partitionerSizes) yield {
    (0 until size).map { _ => random.nextInt(keySize) }.toArray
  }

  // Data for single partition tests
  val rangePartition = new RangePartition(0, 1050, 11050) // 10000 keys in total
  val cyclicPartition = new CyclicPartition(3, 9, 90000) // 10000 keys in total
  val singleSizes = Gen.range("size")(5000, 10000, 1000)
  val rangePartitionData = (0 until 10000).map { case x => x + 1050 }.toArray
  val cyclicPartitionData = (0 until 10000).map { case x => (x * 9) + 3 }.toArray

  // Number of benchmark runs
  val benchRuns = 20

  performance of "RangePartitioner" in {
    measure method "partition" config (
     exec.benchRuns -> benchRuns
    ) in {
      using(partitionerData) in {
        keys =>
          var i = 0
          while (i < keys.length) {
            rangePartitioner.partition(keys(i))
            i += 1
          }
      }
    }
  }

  performance of "CyclicPartitioner" in {
    measure method "partition" config (
      exec.benchRuns -> benchRuns
      ) in {
      using(partitionerData) in {
        keys =>
          var i = 0
          while (i < keys.length) {
            cyclicPartitioner.partition(keys(i))
            i += 1
          }
      }
    }
  }

  performance of "CyclicPartition" in {
    measure method "globalToLocal" config (
      exec.benchRuns -> benchRuns
    ) in {
      using(singleSizes) in { size =>
        var i = 0
        while (i < size) {
          cyclicPartition.globalToLocal(cyclicPartitionData(i))
          i += 1
        }
      }
    }
  }

  performance of "CyclicPartition" in {
    measure method "localToGlobal" config (
      exec.benchRuns -> benchRuns
    ) in {
      using(singleSizes) in { size =>
        var i = 0
        while (i < size) {
          cyclicPartition.localToGlobal(cyclicPartitionData(i))
          i += 1
        }
      }
    }
  }

  performance of "RangePartition" in {
    measure method "globalToLocal" config (
      exec.benchRuns -> benchRuns
    ) in {
      using(singleSizes) in { size =>
        var i = 0
        while (i < size) {
          rangePartition.globalToLocal(rangePartitionData(i))
          i += 1
        }
      }
    }
  }

   performance of "RangePartition" in {
    measure method "localToGlobal" config (
      exec.benchRuns -> benchRuns
    ) in {
      using(singleSizes) in { size =>
        var i = 0
        while (i < size) {
          rangePartition.localToGlobal(rangePartitionData(i))
          i += 1
        }
      }
    }
  }
}
