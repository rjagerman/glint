package glint.serialization

import java.nio.ByteBuffer

class ExtendedByteBuffer(val buf: ByteBuffer) {

  def putLongArray(values: Array[Long]): Unit = {
    val typedBuffer = buf.asLongBuffer()
    typedBuffer.put(values)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfLong)
  }

  def putIntArray(values: Array[Int]): Unit = {
    val typedBuffer = buf.asIntBuffer()
    typedBuffer.put(values)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfInt)
  }

  def putDoubleArray(values: Array[Double]): Unit = {
    val typedBuffer = buf.asDoubleBuffer()
    typedBuffer.put(values)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfDouble)
  }

  def putFloatArray(values: Array[Float]): Unit = {
    val typedBuffer = buf.asFloatBuffer()
    typedBuffer.put(values)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfFloat)
  }

  def getLongArray(size: Int): Array[Long] = {
    val output = new Array[Long](size)
    val typedBuffer = buf.asLongBuffer()
    typedBuffer.get(output)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfLong)
    output
  }

  def getIntArray(size: Int): Array[Int] = {
    val output = new Array[Int](size)
    val typedBuffer = buf.asIntBuffer()
    typedBuffer.get(output)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfInt)
    output
  }

  def getDoubleArray(size: Int): Array[Double] = {
    val output = new Array[Double](size)
    val typedBuffer = buf.asDoubleBuffer()
    typedBuffer.get(output)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfDouble)
    output
  }

  def getFloatArray(size: Int): Array[Float] = {
    val output = new Array[Float](size)
    val typedBuffer = buf.asFloatBuffer()
    typedBuffer.get(output)
    buf.position(buf.position() + typedBuffer.position() * SerializationConstants.sizeOfFloat)
    output
  }

}

object ExtendedByteBuffer {
  implicit def byteBufferToExtendedByteBuffer(b: ByteBuffer): ExtendedByteBuffer = new ExtendedByteBuffer(b)
}
