package glint

import java.util.concurrent.Executors

import com.typesafe.config.Config
import org.apache.spark.rdd.RDD

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * Implicit imports for easy integration with RDDs
  */
package object implicits {

  /**
    * Provides basic parameter server ease-of-use improvements to RDDs
    *
    * @param rdd The RDD on which a function is called
    * @tparam A The type of samples stored in the RDD
    */
  implicit class RDDImprovements[A: ClassTag](val rdd: RDD[A]) {

    /**
      * Iterates an RDD with a parameter server for given number of iterations
      *
      * @param config The configuration with which to construct the parameter server client
      * @param iterations The number of iterations to run
      * @param func A function taking arguments (iteration: Int, partition: Int, client: Client, samples: Iterator[A])
      */
    def iteratePartitionsWithGlint(config: Config,
                                   iterations: Int)
                                  (func: (Int, Int, Client, Iterator[A]) => Iterator[A]): RDD[A] = {
      val nrOfPartitions = rdd.partitions.length
      var rddNext = rdd
      for (t <- 0 until iterations) {
        rddNext = rddNext.mapPartitionsWithIndex {
          case (partition, it) =>
            implicit val ec = ExecutionContext.Implicits.global
            val client = Await.result(Client(config), 120 seconds)
            val output = func(t, partition, client, it)
            client.stop()
            output
        }
      }
      rddNext foreachPartition { case it => }
      rddNext
    }

    /**
     * Executes a function for each partition while providing a client interface to the parameter server
     *
     * @param config The parameter server config
     * @param func The function to execute
     */
    def foreachPartitionWithGlint(config: Config)(func: (Int, Client, Iterator[A]) => Unit): Unit = {
      rdd foreachPartitionWithIndex {
        case (partition, it) =>
          implicit val ec = ExecutionContext.Implicits.global
          val client = Await.result(Client(config), 120 seconds)
          func(partition, client, it)
          client.stop()
      }
    }

    /**
      * Performs a basic foreachPartition with indices
      *
      * @param func The function to execute
      */
    def foreachPartitionWithIndex(func: (Int, Iterator[A]) => Unit): Unit = {
      rdd.mapPartitionsWithIndex { (id, it) =>
        func(id, it)
        Array.empty[Int].toIterator
      }.foreachPartition {it => }
    }

  }

}
