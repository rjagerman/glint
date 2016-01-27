package glint.serialization

/**
  * A very fast primitive deserializer using sun's Unsafe to directly read/write memory regions in the JVM
  *
  * @param bytes The serialized data
  */
private[glint] class FastPrimitiveDeserializer(bytes: Array[Byte]) {

  private val unsafe = SerializationConstants.unsafe
  private val offset = unsafe.arrayBaseOffset(classOf[Array[Byte]])
  private var position: Long = 0

  @inline
  def readFloat(): Float = {
    position += SerializationConstants.sizeOfFloat
    unsafe.getFloat(bytes, offset + position - SerializationConstants.sizeOfFloat)
  }

  @inline
  def readDouble(): Double = {
    position += SerializationConstants.sizeOfDouble
    unsafe.getDouble(bytes, offset + position - SerializationConstants.sizeOfDouble)
  }

  @inline
  def readInt(): Int = {
    position += SerializationConstants.sizeOfInt
    unsafe.getInt(bytes, offset + position - SerializationConstants.sizeOfInt)
  }

  @inline
  def readLong(): Long = {
    position += SerializationConstants.sizeOfLong
    unsafe.getLong(bytes, offset + position - SerializationConstants.sizeOfLong)
  }

  @inline
  def readByte(): Byte = {
    position += SerializationConstants.sizeOfByte
    unsafe.getByte(bytes, offset + position - SerializationConstants.sizeOfByte)
  }

  @inline
  def readArrayInt(size: Int): Array[Int] = {
    val array = new Array[Int](size)
    unsafe.copyMemory(bytes, offset + position, array, unsafe.arrayBaseOffset(classOf[Array[Int]]), size * SerializationConstants.sizeOfInt)
    position += size * SerializationConstants.sizeOfInt
    array
  }

  @inline
  def readArrayLong(size: Int): Array[Long] = {
    val array = new Array[Long](size)
    unsafe.copyMemory(bytes, offset + position, array, unsafe.arrayBaseOffset(classOf[Array[Long]]), size * SerializationConstants.sizeOfLong)
    position += size * SerializationConstants.sizeOfLong
    array
  }

  @inline
  def readArrayFloat(size: Int): Array[Float] = {
    val array = new Array[Float](size)
    unsafe.copyMemory(bytes, offset + position, array, unsafe.arrayBaseOffset(classOf[Array[Float]]), size * SerializationConstants.sizeOfFloat)
    position += size * SerializationConstants.sizeOfFloat
    array
  }

  @inline
  def readArrayDouble(size: Int): Array[Double] = {
    val array = new Array[Double](size)
    unsafe.copyMemory(bytes, offset + position, array, unsafe.arrayBaseOffset(classOf[Array[Double]]), size * SerializationConstants.sizeOfDouble)
    position += size * SerializationConstants.sizeOfDouble
    array
  }

}
