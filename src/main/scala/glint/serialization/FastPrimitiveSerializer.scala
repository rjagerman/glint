package glint.serialization

/**
  * A very fast primitive serializer using sun's Unsafe to directly read/write memory regions in the JVM
  *
  * @param size The size of the serialized output (in bytes)
  */
private[glint] class FastPrimitiveSerializer(size: Int) {

  val bytes = new Array[Byte](size)

  private val unsafe = SerializationConstants.unsafe
  private val offset = unsafe.arrayBaseOffset(classOf[Array[Byte]])
  private var position: Long = 0

  @inline
  def reset(): Unit = position = 0L

  @inline
  def writeFloat(value: Float): Unit = {
    unsafe.putFloat(bytes, offset + position, value)
    position += SerializationConstants.sizeOfFloat
  }

  @inline
  def writeInt(value: Int): Unit = {
    unsafe.putInt(bytes, offset + position, value)
    position += SerializationConstants.sizeOfInt
  }

  @inline
  def writeByte(value: Byte): Unit = {
    unsafe.putByte(bytes, offset + position, value)
    position += SerializationConstants.sizeOfByte
  }

  @inline
  def writeLong(value: Long): Unit = {
    unsafe.putLong(bytes, offset + position, value)
    position += SerializationConstants.sizeOfLong
  }

  @inline
  def writeDouble(value: Double): Unit = {
    unsafe.putDouble(bytes, offset + position, value)
    position += SerializationConstants.sizeOfDouble
  }

  @inline
  def writeArrayInt(value: Array[Int]): Unit = {
    unsafe.copyMemory(value, unsafe.arrayBaseOffset(classOf[Array[Int]]), bytes, offset + position, value.length * SerializationConstants.sizeOfInt)
    position += value.length * SerializationConstants.sizeOfInt
  }

  @inline
  def writeArrayLong(value: Array[Long]): Unit = {
    unsafe.copyMemory(value, unsafe.arrayBaseOffset(classOf[Array[Long]]), bytes, offset + position, value.length * SerializationConstants.sizeOfLong)
    position += value.length * SerializationConstants.sizeOfLong
  }

  @inline
  def writeArrayFloat(value: Array[Float]): Unit = {
    unsafe.copyMemory(value, unsafe.arrayBaseOffset(classOf[Array[Float]]), bytes, offset + position, value.length * SerializationConstants.sizeOfFloat)
    position += value.length * SerializationConstants.sizeOfFloat
  }

  @inline
  def writeArrayDouble(value: Array[Double]): Unit = {
    unsafe.copyMemory(value, unsafe.arrayBaseOffset(classOf[Array[Double]]), bytes, offset + position, value.length * SerializationConstants.sizeOfDouble)
    position += value.length * SerializationConstants.sizeOfDouble
  }

}
