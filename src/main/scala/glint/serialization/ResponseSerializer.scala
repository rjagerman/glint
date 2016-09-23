package glint.serialization

import akka.serialization._
import glint.messages.server.response._

/**
  * A fast serializer for responses
  *
  * Internally this uses a very fast primitive serialization/deserialization routine using sun's Unsafe class for direct
  * read/write access to JVM memory. This might not be portable across different JVMs. If serialization causes problems
  * you can default to JavaSerialization by removing the serialization-bindings in the configuration.
  */
class ResponseSerializer extends Serializer {

  override def identifier: Int = 13371

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val fpd = new FastPrimitiveDeserializer(bytes)
    val objectType = fpd.readByte()
    val objectSize = fpd.readInt()

    objectType match {
      case SerializationConstants.responseDoubleByte =>
        val values = fpd.readArrayDouble(objectSize)
        ResponseDouble(values)

      case SerializationConstants.responseFloatByte =>
        val values = fpd.readArrayFloat(objectSize)
        ResponseFloat(values)

      case SerializationConstants.responseIntByte =>
        val values = fpd.readArrayInt(objectSize)
        ResponseInt(values)

      case SerializationConstants.responseLongByte =>
        val values = fpd.readArrayLong(objectSize)
        ResponseLong(values)
    }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case x: ResponseDouble =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * SerializationConstants.sizeOfDouble)
        fps.writeByte(SerializationConstants.responseDoubleByte)
        fps.writeInt(x.values.length)
        fps.writeArrayDouble(x.values)
        fps.bytes

      case x: ResponseRowsDouble =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * x.columns * SerializationConstants.sizeOfDouble)
        fps.writeByte(SerializationConstants.responseDoubleByte)
        fps.writeInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          fps.writeArrayDouble(x.values(i))
          i += 1
        }
        fps.bytes

      case x: ResponseFloat =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * SerializationConstants.sizeOfFloat)
        fps.writeByte(SerializationConstants.responseFloatByte)
        fps.writeInt(x.values.length)
        fps.writeArrayFloat(x.values)
        fps.bytes

      case x: ResponseRowsFloat =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * x.columns * SerializationConstants.sizeOfFloat)
        fps.writeByte(SerializationConstants.responseFloatByte)
        fps.writeInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          fps.writeArrayFloat(x.values(i))
          i += 1
        }
        fps.bytes

      case x: ResponseInt =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * SerializationConstants.sizeOfInt)
        fps.writeByte(SerializationConstants.responseIntByte)
        fps.writeInt(x.values.length)
        fps.writeArrayInt(x.values)
        fps.bytes

      case x: ResponseRowsInt =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * x.columns * SerializationConstants.sizeOfInt)
        fps.writeByte(SerializationConstants.responseIntByte)
        fps.writeInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          fps.writeArrayInt(x.values(i))
          i += 1
        }
        fps.bytes

      case x: ResponseLong =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * SerializationConstants.sizeOfLong)
        fps.writeByte(SerializationConstants.responseLongByte)
        fps.writeInt(x.values.length)
        fps.writeArrayLong(x.values)
        fps.bytes

      case x: ResponseRowsLong =>
        val fps = new FastPrimitiveSerializer(5 + x.values.length * x.columns * SerializationConstants.sizeOfLong)
        fps.writeByte(SerializationConstants.responseLongByte)
        fps.writeInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          fps.writeArrayLong(x.values(i))
          i += 1
        }
        fps.bytes
    }
  }
}
