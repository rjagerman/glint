package glint

import breeze.linalg.DenseVector
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.concurrent.ScalaFutures
import spire.implicits._

/**
  * BigModel test specification
  */
class BigModelSpec extends FlatSpec with SystemTest with Matchers {

  "A BigModel" should "store Double values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.3)) { case a => a }
        whenReady(bigModel.pushSingle(20, 0.5)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == 0.8) // 0.3 + 0.5 = 0.8
      }
    }
  }

  it should "store Int values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Int]("test", 100, 5)) { case a => a }
        whenReady(bigModel.pushSingle(20, 3)) { case p => p }
        whenReady(bigModel.pushSingle(76, -100)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == 8) // 5 + 3 = 8
        assert(whenReady(bigModel.pullSingle(76)) { case a => a } == -95) // 5 + -100 = -95
      }
    }
  }

  it should "store Long values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Long]("test", 100, 8000000000L)) { case a => a }
        whenReady(bigModel.pushSingle(20, 3)) { case p => p }
        whenReady(bigModel.pushSingle(76, -100)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == 8000000003L) // 8000000000 + 3 = 8000000003
        assert(whenReady(bigModel.pullSingle(76)) { case a => a } == 7999999900L) // 8000000000 + -100 = 7999999900
      }
    }
  }

  it should "store DenseVector[Double] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Double]("test", 100, DenseVector.ones[Double](10))) { case a => a }
        val difference = DenseVector.zeros[Double](10)
        difference(3) = 0.9
        difference(7) = -0.3
        val expected = DenseVector.ones[Double](10) + difference
        whenReady(bigModel.pushSingle(20, difference)) { case p => p }
        assert(whenReady(bigModel.pullSingle(20)) { case a => a } == expected)
      }
    }
  }

  it should "store DenseVector[Int] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Int]("test", 100, DenseVector.ones[Int](10))) { case a => a }
        val difference = DenseVector.zeros[Int](10)
        difference(3) = 8
        difference(7) = -3
        val expected = DenseVector.ones[Int](10) + difference
        whenReady(bigModel.pushSingle(42, difference)) { case p => p }
        assert(whenReady(bigModel.pullSingle(42)) { case a => a } == expected)
      }
    }
  }

  it should "store DenseVector[Long] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Long]("test", 100, DenseVector.ones[Long](10))) { case a => a }
        val difference = DenseVector.zeros[Long](10)
        difference(3) = 80000000009L
        difference(7) = -34113L
        val expected = DenseVector.ones[Long](10) + difference
        whenReady(bigModel.pushSingle(42, difference)) { case p => p }
        assert(whenReady(bigModel.pullSingle(42)) { case a => a } == expected)
      }
    }
  }

  it should "store multiple values in a single Push request" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Int]("test", 100, 5)) { case a => a }
        whenReady(bigModel.push(Array(0, 1, 2, 3), Array(34, 32, 30, 28))) { case p => p }
        val result = whenReady(bigModel.pull(Array(0, 1, 2, 3))) { identity }
        result should equal (Array(39, 37, 35, 33))
      }
    }
  }



}