package glint

import breeze.linalg.DenseVector
import glint.models.client.{CachedBigModel, BoundedBigModel}
import org.scalatest.{FlatSpec, Matchers}
import spire.implicits._

/**
  * BigModel test specification
  */
class BigModelSpec extends FlatSpec with SystemTest with Matchers {

  "A BigModel" should "store Double values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.3)) { identity }
        whenReady(bigModel.pushSingle(20, 0.5)) { identity }
        assert(whenReady(bigModel.pullSingle(20)) { identity } == 0.8) // 0.3 + 0.5 = 0.8
      }
    }
  }

  it should "store Int values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Int]("test", 100, 5)) { identity }
        whenReady(bigModel.pushSingle(20, 3)) { identity }
        whenReady(bigModel.pushSingle(76, -100)) { identity }
        assert(whenReady(bigModel.pullSingle(20)) { identity } == 8) // 5 + 3 = 8
        assert(whenReady(bigModel.pullSingle(76)) { identity } == -95) // 5 + -100 = -95
      }
    }
  }

  it should "store Long values in a scalar model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Long]("test", 100, 8000000000L)) { identity }
        whenReady(bigModel.pushSingle(20, 3)) { identity }
        whenReady(bigModel.pushSingle(76, -100)) { identity }
        assert(whenReady(bigModel.pullSingle(20)) { identity } == 8000000003L) // 8000000000 + 3 = 8000000003
        assert(whenReady(bigModel.pullSingle(76)) { identity } == 7999999900L) // 8000000000 + -100 = 7999999900
      }
    }
  }

  it should "store DenseVector[Double] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Double]("test", 100, DenseVector.ones[Double](10))) { identity }
        val difference = DenseVector.zeros[Double](10)
        difference(3) = 0.9
        difference(7) = -0.3
        val expected = DenseVector.ones[Double](10) + difference
        whenReady(bigModel.pushSingle(20, difference)) { identity }
        assert(whenReady(bigModel.pullSingle(20)) { identity } == expected)
      }
    }
  }

  it should "store DenseVector[Int] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Int]("test", 100, DenseVector.ones[Int](10))) { identity }
        val difference = DenseVector.zeros[Int](10)
        difference(3) = 8
        difference(7) = -3
        val expected = DenseVector.ones[Int](10) + difference
        whenReady(bigModel.pushSingle(42, difference)) { identity }
        assert(whenReady(bigModel.pullSingle(42)) { identity } == expected)
      }
    }
  }

  it should "store DenseVector[Long] values a vector model" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Long]("test", 100, DenseVector.ones[Long](10))) { identity }
        val difference = DenseVector.zeros[Long](10)
        difference(3) = 80000000009L
        difference(7) = -34113L
        val expected = DenseVector.ones[Long](10) + difference
        whenReady(bigModel.pushSingle(42, difference)) { identity }
        assert(whenReady(bigModel.pullSingle(42)) { identity } == expected)
      }
    }
  }

  it should "store multiple values in a single Push request" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Int]("test", 100, 5)) { identity }
        whenReady(bigModel.push(Array(0, 1, 2, 3), Array(34, 32, 30, 28))) { identity }
        val result = whenReady(bigModel.pull(Array(0, 1, 2, 3))) { identity }
        result should equal (Array(39, 37, 35, 33))
      }
    }
  }

  "A BoundedBigModel" should "limit the number of simultaneous requests to 1 when set" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel("test", 50000, 0L)) { identity }
        val boundedBigModel = BoundedBigModel(bigModel, 1)
        val keys = (0 until 50000).map(x => x.toLong).toArray
        val values = Array.fill(50000)(133L)
        for (i <- 0 until 20) {
          boundedBigModel.push(keys, values)
          assert(boundedBigModel.processing <= 1)
        }
      }
    }
  }

  it should "limit the number of simultaneous requests to 2 when set" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel("test", 50000, 0L)) { identity }
        val boundedBigModel = BoundedBigModel(bigModel, 2)
        val keys = (0 until 50000).map(x => x.toLong).toArray
        val values = Array.fill(50000)(133L)
        for (i <- 0 until 40) {
          boundedBigModel.push(keys, values)
          assert(boundedBigModel.processing <= 2)
        }
      }
    }
  }

  it should "limit the number of simultaneous requests to 4 when set" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel("test", 50000, 0L)) { identity }
        val boundedBigModel = BoundedBigModel(bigModel, 4)
        val keys = (0 until 50000).map(x => x.toLong).toArray
        val values = Array.fill(50000)(133L)
        for (i <- 0 until 80) {
          boundedBigModel.push(keys, values)
          assert(boundedBigModel.processing <= 4)
        }
      }
    }
  }

  it should "limit the number of simultaneous requests to 8 when set" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel("test", 50000, 0L)) { identity }
        val boundedBigModel = BoundedBigModel(bigModel, 8)
        val keys = (0 until 50000).map(x => x.toLong).toArray
        val values = Array.fill(50000)(133L)
        for (i <- 0 until 160) {
          boundedBigModel.push(keys, values)
          assert(boundedBigModel.processing <= 8)
        }
      }
    }
  }

  "A CachedBigModel" should "cache at most 3 push requests when set" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel("test", 50000, 0L)) { identity }
        val cachedBigModel = CachedBigModel[Long, Long](bigModel, (x,y) => x + y, 3, 5)
        cachedBigModel.pushSingle(0, 5)
        cachedBigModel.pushSingle(1, 133)
        cachedBigModel.pushSingle(2, 799)
        assert(whenReady(bigModel.pullSingle(0)) { identity } == 0)
        assert(whenReady(bigModel.pullSingle(1)) { identity } == 0)
        assert(whenReady(bigModel.pullSingle(2)) { identity } == 0)
        whenReady(cachedBigModel.pushSingle(3, 100)) { identity }
        assert(whenReady(bigModel.pullSingle(0)) { identity } == 5)
        assert(whenReady(bigModel.pullSingle(1)) { identity } == 133)
        assert(whenReady(bigModel.pullSingle(2)) { identity } == 799)
      }
    }
  }

  it should "flush when requested" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel("test", 50000, 0L)) { identity }
        val cachedBigModel = CachedBigModel[Long, Long](bigModel, (x,y) => x + y, 10, 5)
        cachedBigModel.push(Array(0, 1, 2, 3, 4), Array(5, 133, 799, 100, 50))
        val preFlush = whenReady(bigModel.pull(Array(0, 1, 2, 3, 4))) { identity }
        preFlush should equal (Array(0L, 0L, 0L, 0L, 0L))
        whenReady(cachedBigModel.flush) { identity }
        val postFlush = whenReady(bigModel.pull(Array(0, 1, 2, 3, 4))) { identity }
        postFlush should equal (Array(5L, 133L, 799L, 100L, 50L))
      }
    }
  }

}