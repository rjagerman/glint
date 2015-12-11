package glint

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import glint.models.client.BigVector
import org.scalatest.{FlatSpec, Matchers}

/**
  * BigVector test specification
  */
class BigVectorSpec extends FlatSpec with SystemTest with Matchers {

  "A BigVector" should "store Double values" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = whenReady(client.vector[Double](1000)) {
          identity
        }
        val result = whenReady(model.push(Array(0L, 999L), Array(0.54, -0.9999))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(0L, 999L))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(0.54, -0.9999))
      }
    }
  }

  it should "store Float values" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = whenReady(client.vector[Double](9)) {
          identity
        }
        val result = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0.0, -0.001, 100.001, 3.14152))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(0L, 2L, 5L, 8L))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(0.0, -0.001, 100.001, 3.14152))
      }
    }
  }

  it should "store Int values" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = whenReady(client.vector[Int](1000)) {
          identity
        }
        val result = whenReady(model.push(Array(0L, 999L, 99L, 98L, 100L), Array(1090807, -23, 100, 45, 90))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(0L, 999L, 99L, 98L, 100L))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(1090807, -23, 100, 45, 90))
      }
    }
  }

  it should "store Long values" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = whenReady(client.vector[Long](9)) {
          identity
        }
        val result = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(0, -1, 900800700600L, -100200300400500L))) {
          identity
        }
        assert(result)
        val future = model.pull(Array(0L, 2L, 5L, 8L))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(0, -1, 900800700600L, -100200300400500L))
      }
    }
  }

  it should "aggregate values through addition" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val model = whenReady(client.vector[Int](100)) {
          identity
        }
        val result1 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(10, 10, 20, 30))) {
          identity
        }
        val result2 = whenReady(model.push(Array(0L, 2L, 5L, 8L), Array(1, -1, 2, 3))) {
          identity
        }
        assert(result1)
        assert(result2)
        val future = model.pull(Array(0L, 2L, 5L, 8L))
        val value = whenReady(future) {
          identity
        }
        value should equal(Array(11, 9, 22, 33))
      }
    }
  }

  it should "deserialize without an ActorSystem in scope" in {
    var ab: Array[Byte] = Array.empty[Byte]
    withMaster { _ =>
      withServer { _ =>
        withClient { client =>
          val model = whenReady(client.vector[Int](10)) {
            identity
          }
          val bos = new ByteArrayOutputStream
          val out = new ObjectOutputStream(bos)
          out.writeObject(model)
          out.close()
          ab = bos.toByteArray

          val bis = new ByteArrayInputStream(ab)
          val in = new ObjectInputStream(bis)
          val vector = in.readObject().asInstanceOf[BigVector[Int]]
          whenReady(vector.push(Array(0L), Array(42))) {
            identity
          }
          val result = whenReady(vector.pull(Array(0L))) {
            identity
          }
          result should equal(Array(42))
        }
      }
    }
  }

}
