package glint

import glint.models.client.buffered.BufferedBigMatrix
import org.scalatest.{FlatSpec, Matchers}

/**
  * BufferedBigMatrix test specification
  */
class BufferedBigMatrixSpec extends FlatSpec with SystemTest with Matchers {

  "A BufferedBigMatrix" should "buffer values before pushing" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Double](49, 6)
        val bufferedModel = new BufferedBigMatrix[Double](model, 4)
        bufferedModel.bufferedPush(0, 1, 0.54)
        bufferedModel.bufferedPush(48, 5, -0.33)
        bufferedModel.bufferedPush(0, 1, 1.5)

        val oldResult = whenReady(bufferedModel.pull(Array(0L, 48L), Array(1, 5))) {
          identity
        }
        oldResult should equal(Array(0.0, 0.0))

        val push = whenReady(bufferedModel.flush()) {
          identity
        }
        assert(push)

        val future = bufferedModel.pull(Array(0L, 48L), Array(1, 5))
        val newResult = whenReady(future) {
          identity
        }
        newResult should equal(Array(2.04, -0.33))
      }
    }
  }

}
