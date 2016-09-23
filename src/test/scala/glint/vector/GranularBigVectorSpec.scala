package glint.vector

import scala.util.Random

import glint.SystemTest
import glint.models.client.granular.GranularBigVector
import org.scalatest.{FlatSpec, Matchers}

/**
 * GranularBigVector test specification
 */
class GranularBigVectorSpec extends FlatSpec with SystemTest with Matchers {

  "A GranularBigVector" should "handle large push/pull requests" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val size = 1e6.toInt
        val rng = new Random()
        rng.setSeed(42)
        val model = client.vector[Double](size)
        val granularModel = new GranularBigVector(model, 1000)
        val keys = (0 until size).map(_.toLong).toArray
        val values = Array.fill(size) { rng.nextDouble() }

        whenReady(granularModel.push(keys, values)) {
          identity
        }
        val result = whenReady(granularModel.pull(keys)) {
          identity
        }

        result shouldEqual values
      }
    }
  }

  it should "have non-zero max message size" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.vector[Double](10)
        intercept[IllegalArgumentException] {
          val granularModel = new GranularBigVector(model, 0)
        }
        intercept[IllegalArgumentException] {
          val granularModel = new GranularBigVector(model, -1)
        }
      }
    }
  }

}