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

        // Construct a buffered big matrix with a buffer size of 4
        val bufferedModel = new BufferedBigMatrix[Double](model, 4)

        // Perform 3 pushes into the buffer
        bufferedModel.bufferedPush(0, 1, 0.54)
        bufferedModel.bufferedPush(48, 5, -0.33)
        bufferedModel.bufferedPush(0, 1, 1.5)

        // Assert that nothing has been pushed to the parameter servers yet
        val oldResult = whenReady(bufferedModel.pull(Array(0L, 48L), Array(1, 5))) {
          identity
        }
        oldResult should equal(Array(0.0, 0.0))

        // Flush results to the parameter server
        val push = whenReady(bufferedModel.flush()) {
          identity
        }
        assert(push)

        // Assert that the results are now on the parameter server
        val future = bufferedModel.pull(Array(0L, 48L), Array(1, 5))
        val newResult = whenReady(future) {
          identity
        }
        newResult should equal(Array(2.04, -0.33))
      }
    }
  }

  it should "automatically flush values when the buffer is full" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Double](49, 6)

        // Construct a buffered big matrix with a buffer size of 4
        val bufferedModel = new BufferedBigMatrix[Double](model, 4)

        // Perform 3 pushes into the buffer
        bufferedModel.bufferedPush(0, 1, 0.54)
        bufferedModel.bufferedPush(48, 5, -0.33)
        bufferedModel.bufferedPush(0, 1, 1.5)

        // Assert that nothing has been pushed to the parameter servers yet
        val oldResult = whenReady(bufferedModel.pull(Array(0L, 48L), Array(1, 5))) {
          identity
        }
        oldResult should equal(Array(0.0, 0.0))

        // Push another into the buffer, this time the buffer of size 4 is full and should automatically flush
        val finalPush = bufferedModel.bufferedPush(0, 1, 0.3)
        assert(finalPush.isDefined)
        assert(whenReady(finalPush.get) {
          identity
        })

        // Assert that the results are now on the parameter server
        val future = bufferedModel.pull(Array(0L, 48L), Array(1, 5))
        val newResult = whenReady(future) {
          identity
        }
        newResult should equal(Array(2.34, -0.33))
      }
    }
  }

  it should "automatically flush values multiple times whenever the buffer is full" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Double](49, 6)

        // Construct a buffered big matrix with a buffer size of 4
        val bufferedModel = new BufferedBigMatrix[Double](model, 4)

        // Perform 4 pushes into the buffer, causing one flush
        bufferedModel.bufferedPush(0, 1, 0.54)
        bufferedModel.bufferedPush(48, 5, -0.33)
        bufferedModel.bufferedPush(0, 1, 1.5)
        assert(bufferedModel.bufferedPush(0, 1, 0.3).isDefined)

        // Perform another 4 pushes into the buffer, causing one flush
        bufferedModel.bufferedPush(0, 1, 0.54)
        bufferedModel.bufferedPush(48, 5, -0.33)
        bufferedModel.bufferedPush(0, 1, 1.5)
        assert(bufferedModel.bufferedPush(0, 1, 0.3).isDefined)

        // Perform final 4 pushes into the buffer, causing another flush
        bufferedModel.bufferedPush(0, 1, 0.54)
        bufferedModel.bufferedPush(48, 5, -0.33)
        bufferedModel.bufferedPush(0, 1, 1.5)
        val finalPush = bufferedModel.bufferedPush(0, 1, 0.3)
        assert(finalPush.isDefined)
        assert(whenReady(finalPush.get) {
          identity
        })

        // Assert that the results are now on the parameter server
        val future = bufferedModel.pull(Array(0L, 48L), Array(1, 5))
        val newResult = whenReady(future) {
          identity
        }
        newResult should equal(Array(7.02, -0.99))
      }
    }
  }

}
