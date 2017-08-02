package glint.serialization

import java.nio.ByteBuffer

import glint.messages.server.response._
import glint.serialization.ExtendedByteBuffer._

/**
  * A fast serializer for responses
  *
  * Internally this uses a very fast primitive serialization/deserialization routine using sun's Unsafe class for direct
  * read/write access to JVM memory. This might not be portable across different JVMs. If serialization causes problems
  * you can default to JavaSerialization by removing the serialization-bindings in the configuration.
  */
class ResponseSerializer extends GlintSerializer {

  override def identifier: Int = 13371

  override def toBinary(o: AnyRef, buf: ByteBuffer): Unit = {
    o match {
      case x: ResponseDouble =>
        buf.put(SerializationConstants.responseDoubleByte)
        buf.putInt(x.values.length)
        buf.putDoubleArray(x.values)

      case x: ResponseRowsDouble =>
        buf.put(SerializationConstants.responseDoubleByte)
        buf.putInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          buf.putDoubleArray(x.values(i))
          i += 1
        }

      case x: ResponseFloat =>
        buf.put(SerializationConstants.responseFloatByte)
        buf.putInt(x.values.length)
        buf.putFloatArray(x.values)

      case x: ResponseRowsFloat =>
        buf.put(SerializationConstants.responseFloatByte)
        buf.putInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          buf.putFloatArray(x.values(i))
          i += 1
        }

      case x: ResponseInt =>
        buf.put(SerializationConstants.responseIntByte)
        buf.putInt(x.values.length)
        buf.putIntArray(x.values)

      case x: ResponseRowsInt =>
        buf.put(SerializationConstants.responseIntByte)
        buf.putInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          buf.putIntArray(x.values(i))
          i += 1
        }

      case x: ResponseLong =>
        buf.put(SerializationConstants.responseLongByte)
        buf.putInt(x.values.length)
        buf.putLongArray(x.values)

      case x: ResponseRowsLong =>
        buf.put(SerializationConstants.responseLongByte)
        buf.putInt(x.values.length * x.columns)
        var i = 0
        while (i < x.values.length) {
          buf.putLongArray(x.values(i))
          i += 1
        }
    }
  }

  override def fromBinary(buf: ByteBuffer, manifest: String): AnyRef = {
    val objectType = buf.get()
    val objectSize = buf.getInt()

    objectType match {
      case SerializationConstants.responseDoubleByte =>
        val values = buf.getDoubleArray(objectSize)
        ResponseDouble(values)

      case SerializationConstants.responseFloatByte =>
        val values = buf.getFloatArray(objectSize)
        ResponseFloat(values)

      case SerializationConstants.responseIntByte =>
        val values = buf.getIntArray(objectSize)
        ResponseInt(values)

      case SerializationConstants.responseLongByte =>
        val values = buf.getLongArray(objectSize)
        ResponseLong(values)
    }
  }

}
