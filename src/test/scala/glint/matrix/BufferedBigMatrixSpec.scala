package glint.matrix

import glint.SystemTest
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
        bufferedModel.pushToBuffer(0, 1, 0.54)
        bufferedModel.pushToBuffer(48, 5, -0.33)
        bufferedModel.pushToBuffer(0, 1, 1.5)

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

  it should "stop adding to buffer when it is full" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Double](49, 6)

        // Construct a buffered big matrix with a buffer size of 4
        val bufferedModel = new BufferedBigMatrix[Double](model, 4)

        // Perform 3 pushes into the buffer
        bufferedModel.pushToBuffer(0, 1, 0.54)
        bufferedModel.pushToBuffer(48, 5, -0.33)
        bufferedModel.pushToBuffer(0, 1, 1.5)

        // Assert that nothing has been pushed to the parameter servers yet
        val oldResult = whenReady(bufferedModel.pull(Array(0L, 48L), Array(1, 5))) {
          identity
        }
        oldResult should equal(Array(0.0, 0.0))

        // Push another into the buffer
        assert(bufferedModel.pushToBuffer(0, 1, 0.3))

        // Attempting to push another value should return false
        assert(!bufferedModel.pushToBuffer(0, 1, 0.3))

        // Flush the values (without the last one, it was not added because the buffer was full)
        whenReady(bufferedModel.flush()) { identity }

        // Assert that the results are now on the parameter server
        val future = bufferedModel.pull(Array(0L, 48L), Array(1, 5))
        val newResult = whenReady(future) {
          identity
        }
        newResult should equal(Array(2.34, -0.33))
      }
    }
  }

  it should "be able to flush the buffer multiple times" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Double](49, 6)

        // Construct a buffered big matrix with a buffer size of 4
        val bufferedModel = new BufferedBigMatrix[Double](model, 4)

        // Perform 4 pushes into the buffer, causing one flush
        bufferedModel.pushToBuffer(0, 1, 0.54)
        bufferedModel.pushToBuffer(48, 5, -0.33)
        bufferedModel.pushToBuffer(0, 1, 1.5)
        bufferedModel.pushToBuffer(0, 1, 0.3)
        bufferedModel.flush()

        // Perform another 4 pushes into the buffer, causing one flush
        bufferedModel.pushToBuffer(0, 1, 0.54)
        bufferedModel.pushToBuffer(48, 5, -0.33)
        bufferedModel.pushToBuffer(0, 1, 1.5)
        bufferedModel.pushToBuffer(0, 1, 0.3)
        bufferedModel.flush()

        // Perform final 4 pushes into the buffer, causing another flush
        bufferedModel.pushToBuffer(0, 1, 0.54)
        bufferedModel.pushToBuffer(48, 5, -0.33)
        bufferedModel.pushToBuffer(0, 1, 1.5)
        bufferedModel.pushToBuffer(0, 1, 0.3)
        whenReady(bufferedModel.flush()) { identity }

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
