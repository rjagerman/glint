package glint.serialization

import java.nio.ByteBuffer

import glint.SystemTest
import glint.messages.server.request.PullMatrix
import glint.messages.server.response.ResponseLong
import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import org.scalameter.{Bench, Gen}

/**
  * Benchmarks serialization
  */
object SerializationBenchmark extends Bench.OfflineReport with SystemTest {

  // Configuration
  override lazy val executor = LocalExecutor(new Executor.Warmer.Default, aggregator, measurer)
  exec.reinstantiation.frequency -> 4

  // Construct serializers
  val requestSerializer = new RequestSerializer()
  val responseSerializer = new ResponseSerializer()

  // Defines number of iterations to test (test size)
  val sizes = Gen.range("size")(1000, 9000, 2000)
  val requestData = for (size <- sizes) yield PullMatrix((0L until size).toArray, (0 until size).toArray)
  val requestDataRequestSerialized = for (size <- sizes) yield requestSerializer.toBinary(PullMatrix((0L until size).toArray, (0 until size).toArray))

  val responseData = for (size <- sizes) yield ResponseLong((0L until size).toArray)
  val responseDataResponseSerialized = for (size <- sizes) yield responseSerializer.toBinary(ResponseLong((0L until size).toArray))

  val benchRuns = 150
  exec.reinstantiation.frequency -> 4

  val buf: ByteBuffer = ByteBuffer.allocateDirect(1024*1024)


  performance of "Request" in {
    performance of "RequestSerializer" in {
      measure method "toBinary" config (exec.benchRuns -> benchRuns) in {
        using(requestData) in {
          d =>
            requestSerializer.toBinary(d, buf)
            buf.clear()
        }
      }
      measure method "fromBinary" config (exec.benchRuns -> benchRuns) in {
        using(requestDataRequestSerialized) setUp {
          d =>
            buf.put(d)
            buf.position(0)
        } tearDown {
          _ => buf.clear()
        } in {
          _ => requestSerializer.fromBinary(buf, "")
        }
      }
    }
  }

  performance of "Response" in {
    performance of "ResponseSerializer" in {
      measure method "toBinary" config (exec.benchRuns -> benchRuns) in {
        using(responseData) tearDown {
          _ => buf.clear()
        } in {
          d => responseSerializer.toBinary(d, buf)
        }
      }
      measure method "fromBinary" config (exec.benchRuns -> benchRuns) in {
        using(responseDataResponseSerialized) setUp {
          d =>
            buf.put(d)
            buf.position(0)
        } tearDown {
          _ => buf.clear()
        } in {
          _ => responseSerializer.fromBinary(buf, "")
        }
      }
    }
  }
}

