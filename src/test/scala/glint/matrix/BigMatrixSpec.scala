package glint.matrix

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import breeze.linalg.DenseVector
import glint.SystemTest
import glint.models.client.BigMatrix
import glint.models.server.aggregate.{AggregateMin, AggregateMax, AggregateReplace}
import org.scalatest.{FlatSpec, Matchers}

/**
  * BigMatrix test specification
  */
class BigMatrixSpec extends FlatSpec with SystemTest with Matchers {

  "A BigMatrix" should "store Double values" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = client.matrix[Double](49, 6)
        val result = whenReady(model.push(Array(0L), Array(1), Array(0.54))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(0L), Array(1))
        val value = whenReady(future) {
          identity
        }
        assert(value(0) == 0.54)
      }
    }
  }

  it should "store Float values" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = client.matrix[Float](49, 6, 8)
        val result = whenReady(model.push(Array(10L, 0L, 48L), Array(0, 1, 5), Array(0.0f, 0.54f, 0.33333f))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(10L, 0L, 48L), Array(0, 1, 5))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(0.0f, 0.54f, 0.33333f))
      }
    }
  }

  it should "store Int values" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = client.matrix[Int](23, 10)
        val result = whenReady(model.push(Array(1L, 5L, 20L), Array(0, 1, 8), Array(0, -1000, 23451234))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(1L, 5L, 20L), Array(0, 1, 8))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(0, -1000, 23451234))
      }
    }
  }

  it should "store Long values" in withMaster { _ =>
    withServers(3) { _ =>
      withClient { client =>
        val model = client.matrix[Long](23, 10)
        val result = whenReady(model.push(Array(1L, 5L, 20L), Array(0, 8, 1), Array(0L, -789300200100L, 987100200300L))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(1L, 5L, 20L), Array(0, 8, 1))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(0L, -789300200100L, 987100200300L))
      }
    }
  }

  it should "return rows as vectors" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Int](100, 100, 3)
        val result1 = whenReady(model.push(Array(0L, 20L, 50L, 81L), Array(0, 10, 99, 80), Array(100, 100, 20, 30))) {
          identity
        }
        val value1 = whenReady(model.pull(Array(0L, 20L, 50L, 81L))) {
          identity
        }
        val result2 = whenReady(model.push(Array(0L, 20L, 50L, 81L), Array(0, 10, 99, 80), Array(1, -1, 2, 3))) {
          identity
        }
        val value = whenReady(model.pull(Array(0L, 20L, 50L, 81L))) {
          identity
        }
        val value0 = DenseVector.zeros[Int](100)
        value0(0) = 101
        val value20 = DenseVector.zeros[Int](100)
        value20(10) = 99
        val value50 = DenseVector.zeros[Int](100)
        value50(99) = 22
        val value81 = DenseVector.zeros[Int](100)
        value81(80) = 33
        assert(value(0) == value0)
        assert(value(1) == value20)
        assert(value(2) == value50)
        assert(value(3) == value81)
      }
    }
  }

  it should "aggregate values through addition by default" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Int](9, 100)
        val result1 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(100, 100, 20, 30))) {
          identity
        }
        val result2 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(1, -1, 2, 3))) {
          identity
        }
        assert(result1)
        assert(result2)
        val future = model.pull(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(101, 99, 22, 33))
      }
    }
  }

  it should "aggregate values through maximum when specified" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Int](9, 100, 1, AggregateMax())
        val result1 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(100, 100, -999, 30))) {
          identity
        }
        val result2 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(1, -1, 20, 300))) {
          identity
        }
        assert(result1)
        assert(result2)
        val future = model.pull(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(100, 100, 20, 300))
      }
    }
  }

  it should "aggregate values through minimum when specified" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Int](9, 100, 1, AggregateMin())
        val result1 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(100, 100, -999, 30))) {
          identity
        }
        val result2 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(1, -1, 20, 300))) {
          identity
        }
        assert(result1)
        assert(result2)
        val future = model.pull(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(0, -1, -999, 0))
      }
    }
  }

  it should "aggregate values through replacement when specified" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Int](9, 100, 1, AggregateReplace())
        val result1 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(100, 100, -999, 30))) {
          identity
        }
        val result2 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80), Array(1, -1, 20, 300))) {
          identity
        }
        assert(result1)
        assert(result2)
        val future = model.pull(Array(0L, 2L, 5L, 8L), Array(0, 10, 99, 80))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(1, -1, 20, 300))
      }
    }
  }

  it should "deserialize without an ActorSystem in scope" in {
    var ab: Array[Byte] = Array.empty[Byte]
    withMaster { _ =>
      withServers(2) { _ =>
        withClient { client =>
          val model = client.matrix[Int](9, 10)
          val bos = new ByteArrayOutputStream
          val out = new ObjectOutputStream(bos)
          out.writeObject(model)
          out.close()
          ab = bos.toByteArray

          whenReady(model.push(Array(0L, 7L), Array(1, 2), Array(12, 42))) {
            identity
          }

          val bis = new ByteArrayInputStream(ab)
          val in = new ObjectInputStream(bis)
          val matrix = in.readObject().asInstanceOf[BigMatrix[Int]]
          val result = whenReady(matrix.pull(Array(0L, 7L), Array(1, 2))) {
            identity
          }
          result should equal(Array(12, 42))
        }
      }
    }
  }

}
