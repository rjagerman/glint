package glint

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import glint.models.client.BigMatrix
import glint.models.client.aggregate.AggregatedBigMatrix
import org.scalatest.{FlatSpec, Matchers}

/**
  * AggregatedBigMatrix test specification
  */
class AggregatedBigMatrixSpec extends FlatSpec with SystemTest with Matchers {

  "An AggregatedBigMatrix" should "aggregate values before pushing" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = whenReady(client.matrix[Double](49, 6)) {
          identity
        }
        val aggregatedModel = new AggregatedBigMatrix[Double](model, (x,y) => x + y, 0.0)
        aggregatedModel.aggregatePush(0, 1, 0.54)
        aggregatedModel.aggregatePush(48, 5, -0.33)
        aggregatedModel.aggregatePush(0, 1, 1.5)

        val oldResult = whenReady(aggregatedModel.pull(Array(0, 48), Array(1, 5))) { identity }
        oldResult should equal(Array(0.0, 0.0))

        val push = whenReady(aggregatedModel.flush()) { identity }
        assert(push)

        val future = aggregatedModel.pull(Array(0L, 48L), Array(1, 5))
        val newResult = whenReady(future) {
          identity
        }
        newResult should equal(Array(2.04, -0.33))
      }
    }
  }

}
