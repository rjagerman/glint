package glint.serialization

import akka.serialization.JavaSerializer
import glint.SystemTest
import glint.messages.server.request.PullMatrix
import glint.messages.server.response.ResponseLong
import org.scalameter.api._
import org.scalameter.{Bench, Gen}

/**
  * Benchmarks serialization
  */
object SerializationBenchmark extends Bench.OfflineReport with SystemTest {

  // Construct serializers
  val requestSerializer = new RequestSerializer()
  val responseSerializer = new ResponseSerializer()
  val defaultJavaSerializer = new JavaSerializer(JavaSerializer.currentSystem.value)

  // Defines number of iterations to test (test size)
  val sizes = Gen.range("size")(1000, 9000, 2000)
  val requestData = for (size <- sizes) yield PullMatrix((0L until size).toArray, (0 until size).toArray)
  val requestDataJavaSerialized = for (size <- sizes) yield defaultJavaSerializer.toBinary(PullMatrix((0L until size).toArray, (0 until size).toArray))
  val requestDataRequestSerialized = for (size <- sizes) yield requestSerializer.toBinary(PullMatrix((0L until size).toArray, (0 until size).toArray))

  val responseData = for (size <- sizes) yield ResponseLong((0L until size).toArray)
  val responseDataJavaSerialized = for (size <- sizes) yield defaultJavaSerializer.toBinary(ResponseLong((0L until size).toArray))
  val responseDataResponseSerialized = for (size <- sizes) yield responseSerializer.toBinary(ResponseLong((0L until size).toArray))

  val benchRuns = 150
  exec.reinstantiation.frequency -> 4

  performance of "Request" in {
    performance of "RequestSerializer" in {
      measure method "toBinary" config (
        exec.benchRuns -> benchRuns
        ) in {
        using(requestData) in {
          d => requestSerializer.toBinary(d)
        }
      }
      measure method "fromBinary" config (
        exec.benchRuns -> benchRuns
        ) in {
        using(requestDataRequestSerialized) in {
          d => requestSerializer.fromBinary(d)
        }
      }
    }

    performance of "JavaSerializer" in {
      measure method "toBinary" config (
        exec.benchRuns -> benchRuns
        ) in {
        using(requestData) in {
          d => defaultJavaSerializer.toBinary(d)
        }
      }
      //      measure method "fromBinary" config (
      //        exec.benchRuns -> benchRuns
      //        ) in {
      //        using(requestDataJavaSerialized) in {
      //          d => defaultJavaSerializer.fromBinary(d)
      //        }
      //      }
    }
  }

  performance of "Response" in {
    performance of "ResponseSerializer" in {
      measure method "toBinary" config (
        exec.benchRuns -> benchRuns
        ) in {
        using(responseData) in {
          d => responseSerializer.toBinary(d)
        }
      }
      measure method "fromBinary" config (
        exec.benchRuns -> benchRuns
        ) in {
        using(responseDataResponseSerialized) in {
          d => responseSerializer.fromBinary(d)
        }
      }
    }
    performance of "JavaSerializer" in {
      measure method "toBinary" config (
        exec.benchRuns -> benchRuns
        ) in {
        using(responseData) in {
          d => defaultJavaSerializer.toBinary(d)
        }
      }
      //      measure method "fromBinary" config (
      //        exec.benchRuns -> benchRuns
      //        ) in {
      //        using(responseDataJavaSerialized) in {
      //          d => defaultJavaSerializer.fromBinary(d)
      //        }
      //      }
    }
  }

}
