package glint.serialization

import glint.messages.server.request._
import glint.messages.server.response._
import org.scalatest.{FlatSpec, Matchers}

/**
  * A serialization spec test
  */
class SerializationSpec extends FlatSpec with Matchers {

  "A RequestSerializer" should "serialize and deserialize a PullMatrix" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PullMatrix(Array(0L, 1L, 2L), Array(3, 4, 5)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PullMatrix])
    val pullMatrix = reconstruction.asInstanceOf[PullMatrix]
    pullMatrix.rows should equal(Array(0L, 1L, 2L))
    pullMatrix.cols should equal(Array(3, 4, 5))
  }

  it should "serialize and deserialize a PullMatrixRows" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PullMatrixRows(Array(0L, 1L, 2L, 5L)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PullMatrixRows])
    val pullMatrixRows = reconstruction.asInstanceOf[PullMatrixRows]
    pullMatrixRows.rows should equal(Array(0L, 1L, 2L, 5L))
  }

  it should "serialize and deserialize a PullVector" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PullVector(Array(0L, 16L, 2L, 5L)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PullVector])
    val pullVector = reconstruction.asInstanceOf[PullVector]
    pullVector.keys should equal(Array(0L, 16L, 2L, 5L))
  }

  it should "serialize and deserialize a PushMatrixDouble" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushMatrixDouble(Array(0L, 5L, 9L), Array(2, 10, 3), Array(0.0, 0.5, 0.99)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushMatrixDouble])
    val pushMatrixDouble = reconstruction.asInstanceOf[PushMatrixDouble]
    pushMatrixDouble.rows should equal(Array(0L, 5L, 9L))
    pushMatrixDouble.cols should equal(Array(2, 10, 3))
    pushMatrixDouble.values should equal(Array(0.0, 0.5, 0.99))
  }

  it should "serialize and deserialize a PushMatrixFloat" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushMatrixFloat(Array(0L, 5L, 9L), Array(2, 10, 3), Array(0.3f, 0.6f, 10.314f)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushMatrixFloat])
    val pushMatrixFloat = reconstruction.asInstanceOf[PushMatrixFloat]
    pushMatrixFloat.rows should equal(Array(0L, 5L, 9L))
    pushMatrixFloat.cols should equal(Array(2, 10, 3))
    pushMatrixFloat.values should equal(Array(0.3f, 0.6f, 10.314f))
  }

  it should "serialize and deserialize a PushMatrixInt" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushMatrixInt(Array(1L, 2L, 100000000000L), Array(10000, 10, 1), Array(99, -20, -3500)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushMatrixInt])
    val pushMatrixInt = reconstruction.asInstanceOf[PushMatrixInt]
    pushMatrixInt.rows should equal(Array(1L, 2L, 100000000000L))
    pushMatrixInt.cols should equal(Array(10000, 10, 1))
    pushMatrixInt.values should equal(Array(99, -20, -3500))
  }

  it should "serialize and deserialize a PushMatrixLong" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushMatrixLong(Array(1L, 2L, 100000000000L), Array(10000, 10, 1), Array(5000300200100L, -9000100200300L, 0L)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushMatrixLong])
    val pushMatrixLong = reconstruction.asInstanceOf[PushMatrixLong]
    pushMatrixLong.rows should equal(Array(1L, 2L, 100000000000L))
    pushMatrixLong.cols should equal(Array(10000, 10, 1))
    pushMatrixLong.values should equal(Array(5000300200100L, -9000100200300L, 0L))
  }

  it should "serialize and deserialize a PushVectorDouble" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushVectorDouble(Array(0L, 5L, 9L), Array(0.0, 0.5, 0.99)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushVectorDouble])
    val pushMatrixDouble = reconstruction.asInstanceOf[PushVectorDouble]
    pushMatrixDouble.keys should equal(Array(0L, 5L, 9L))
    pushMatrixDouble.values should equal(Array(0.0, 0.5, 0.99))
  }

  it should "serialize and deserialize a PushVectorFloat" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushVectorFloat(Array(0L, 5L, 9L), Array(0.3f, 0.6f, 10.314f)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushVectorFloat])
    val pushMatrixFloat = reconstruction.asInstanceOf[PushVectorFloat]
    pushMatrixFloat.keys should equal(Array(0L, 5L, 9L))
    pushMatrixFloat.values should equal(Array(0.3f, 0.6f, 10.314f))
  }

  it should "serialize and deserialize a PushVectorInt" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushVectorInt(Array(1L, 2L, 100000000000L), Array(99, -20, -3500)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushVectorInt])
    val pushMatrixInt = reconstruction.asInstanceOf[PushVectorInt]
    pushMatrixInt.keys should equal(Array(1L, 2L, 100000000000L))
    pushMatrixInt.values should equal(Array(99, -20, -3500))
  }

  it should "serialize and deserialize a PushVectorLong" in {
    val requestSerializer = new RequestSerializer()
    val bytes = requestSerializer.toBinary(PushVectorLong(Array(1L, 2L, 100000000000L), Array(5000300200100L, -9000100200300L, 0L)))
    val reconstruction = requestSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[PushVectorLong])
    val pushMatrixLong = reconstruction.asInstanceOf[PushVectorLong]
    pushMatrixLong.keys should equal(Array(1L, 2L, 100000000000L))
    pushMatrixLong.values should equal(Array(5000300200100L, -9000100200300L, 0L))
  }

  "A ResponseSerializer" should "serialize and deserialize a ResponseDouble" in {
    val responseSerializer = new ResponseSerializer()
    val bytes = responseSerializer.toBinary(ResponseDouble(Array(0.01, 3.1415, -0.999)))
    val reconstruction = responseSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[ResponseDouble])
    val responseDouble = reconstruction.asInstanceOf[ResponseDouble]
    responseDouble.values should equal(Array(0.01, 3.1415, -0.999))
  }

  it should "serialize and deserialize a ResponseFloat" in {
    val responseSerializer = new ResponseSerializer()
    val bytes = responseSerializer.toBinary(ResponseFloat(Array(100.001f, -3.1415f, 0.1234f)))
    val reconstruction = responseSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[ResponseFloat])
    val responseFloat = reconstruction.asInstanceOf[ResponseFloat]
    responseFloat.values should equal(Array(100.001f, -3.1415f, 0.1234f))
  }

  it should "serialize and deserialize a ResponseInt" in {
    val responseSerializer = new ResponseSerializer()
    val bytes = responseSerializer.toBinary(ResponseInt(Array(100, -200, 999123)))
    val reconstruction = responseSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[ResponseInt])
    val responseInt = reconstruction.asInstanceOf[ResponseInt]
    responseInt.values should equal(Array(100, -200, 999123))
  }

  it should "serialize and deserialize a ResponseLong" in {
    val responseSerializer = new ResponseSerializer()
    val bytes = responseSerializer.toBinary(ResponseLong(Array(0L, -200L, 9876300200100L)))
    val reconstruction = responseSerializer.fromBinary(bytes)
    assert(reconstruction.isInstanceOf[ResponseLong])
    val responseInt = reconstruction.asInstanceOf[ResponseLong]
    responseInt.values should equal(Array(0L, -200L, 9876300200100L))
  }

}
