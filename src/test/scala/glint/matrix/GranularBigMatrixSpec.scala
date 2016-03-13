package glint.matrix

import glint.SystemTest
import glint.models.client.granular.GranularBigMatrix
import org.scalatest.{FlatSpec, Matchers}

/**
  * GranularBigMatrix test specification
  */
class GranularBigMatrixSpec extends FlatSpec with SystemTest with Matchers {

  "A GranularBigMatrix" should "handle large push/pull requests" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Double](1000, 1000)
        val granularModel = new GranularBigMatrix[Double](model, 10000)
        val rows = new Array[Long](1000000)
        val cols = new Array[Int](1000000)
        val values = new Array[Double](1000000)
        var i = 0
        while (i < rows.length) {
          rows(i) = i % 1000
          cols(i) = i / 1000
          values(i) = i * 3.14
          i += 1
        }

        whenReady(granularModel.push(rows, cols, values)) {
          identity
        }
        val result = whenReady(granularModel.pull(rows, cols)) {
          identity
        }

        result shouldEqual values
      }
    }
  }

  it should " handle large pull requests for rows"  in withMaster { _ =>
    withServers(3) { _ =>
      withClient { client =>
        val model = client.matrix[Double](1000, 1000)
        val granularModel = new GranularBigMatrix[Double](model, 10000)
        val rows = new Array[Long](1000000)
        val cols = new Array[Int](1000000)
        val values = new Array[Double](1000000)
        var i = 0
        while (i < rows.length) {
          rows(i) = i % 1000
          cols(i) = i / 1000
          values(i) = i * 3.14
          i += 1
        }

        whenReady(granularModel.push(rows, cols, values)) {
          identity
        }
        val result = whenReady(granularModel.pull((0L until 1000L).toArray)) {
          identity
        }

        i = 0
        while (i < rows.length) {
          assert(result(i % 1000)(i / 1000) == values(i))
          i += 1
        }
      }
    }
  }

}
