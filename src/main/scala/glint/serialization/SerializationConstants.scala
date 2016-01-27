package glint.serialization

import sun.misc.Unsafe

/**
  * Some constants used for serialization
  */
private[glint] object SerializationConstants {

  // Unsafe field for direct memory access
  private val field = classOf[Unsafe].getDeclaredField("theUnsafe")
  field.setAccessible(true)
  val unsafe = field.get(null).asInstanceOf[Unsafe]

  // Size of different java primitives to perform direct read/write to java memory
  val sizeOfByte = 1
  val sizeOfShort = 2
  val sizeOfInt = 4
  val sizeOfLong = 8
  val sizeOfFloat = 4
  val sizeOfDouble = 8

  // Byte identifiers for different message types
  val pullMatrixByte: Byte = 0x00
  val pullMatrixRowsByte: Byte = 0x01
  val pullVectorByte: Byte = 0x02
  val pushMatrixDoubleByte: Byte = 0x03
  val pushMatrixFloatByte: Byte = 0x04
  val pushMatrixIntByte: Byte = 0x05
  val pushMatrixLongByte: Byte = 0x06
  val pushVectorDoubleByte: Byte = 0x07
  val pushVectorFloatByte: Byte = 0x08
  val pushVectorIntByte: Byte = 0x09
  val pushVectorLongByte: Byte = 0x0A
  val responseDoubleByte: Byte = 0x10
  val responseFloatByte: Byte = 0x11
  val responseIntByte: Byte = 0x12
  val responseLongByte: Byte = 0x13

}
