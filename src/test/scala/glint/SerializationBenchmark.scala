package glint

import akka.serialization.JavaSerializer
import glint.messages.server.request.PullMatrix
import glint.serialization.{RequestSerializer, ResponseSerializer}
import org.scalameter.api._
import org.scalameter.{Bench, Gen}

/**
  * Benchmarks serialization
  */
object SerializationBenchmark extends Bench.OfflineReport {

  // Construct serializers
  val requestSerializer = new RequestSerializer()
  val responseSerializer = new ResponseSerializer()
  val defaultJavaSerializer = new JavaSerializer(JavaSerializer.currentSystem.value)

  // Defines number of iterations to test (test size)
  val sizes = Gen.range("size")(1000, 9000, 2000)
  val data = for (size <- sizes) yield PullMatrix((0L until size).toArray, (0 until size).toArray)
  val serializedData = for (size <- sizes) yield requestSerializer.toBinary(PullMatrix((0L until size).toArray, (0 until size).toArray))

  performance of "Serialization" in {
    performance of "RequestSerializer" in {
      measure method "toBinary" config (
        exec.benchRuns -> 300
        ) in {
        using(data) in {
          d => requestSerializer.toBinary(d)
        }
      }
    }

    performance of "JavaSerializer" in {
      measure method "toBinary" config (
        exec.benchRuns -> 300
        ) in {
        using(data) in {
          d => defaultJavaSerializer.toBinary(d)
        }
      }
    }
  }

}
