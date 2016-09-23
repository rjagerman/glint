package glint.serialization

import akka.serialization._
import glint.messages.server.request._

/**
  * A fast serializer for requests
  *
  * Internally this uses a very fast primitive serialization/deserialization routine using sun's Unsafe class for direct
  * read/write access to JVM memory. This might not be portable across different JVMs. If serialization causes problems
  * you can default to JavaSerialization by removing the serialization-bindings in the configuration.
  */
class RequestSerializer extends Serializer {

  override def identifier: Int = 13370

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val fpd = new FastPrimitiveDeserializer(bytes)
    val objectType = fpd.readByte()
    val objectSize = fpd.readInt()

    objectType match {
      case SerializationConstants.pullMatrixByte =>
        val rows = fpd.readArrayLong(objectSize)
        val cols = fpd.readArrayInt(objectSize)
        PullMatrix(rows, cols)

      case SerializationConstants.pullMatrixRowsByte =>
        val rows = fpd.readArrayLong(objectSize)
        PullMatrixRows(rows)

      case SerializationConstants.pullVectorByte =>
        val keys = fpd.readArrayLong(objectSize)
        PullVector(keys)

      case SerializationConstants.pushMatrixDoubleByte =>
        val id = fpd.readInt()
        val rows = fpd.readArrayLong(objectSize)
        val cols = fpd.readArrayInt(objectSize)
        val values = fpd.readArrayDouble(objectSize)
        PushMatrixDouble(id, rows, cols, values)

      case SerializationConstants.pushMatrixFloatByte =>
        val id = fpd.readInt()
        val rows = fpd.readArrayLong(objectSize)
        val cols = fpd.readArrayInt(objectSize)
        val values = fpd.readArrayFloat(objectSize)
        PushMatrixFloat(id, rows, cols, values)

      case SerializationConstants.pushMatrixIntByte =>
        val id = fpd.readInt()
        val rows = fpd.readArrayLong(objectSize)
        val cols = fpd.readArrayInt(objectSize)
        val values = fpd.readArrayInt(objectSize)
        PushMatrixInt(id, rows, cols, values)

      case SerializationConstants.pushMatrixLongByte =>
        val id = fpd.readInt()
        val rows = fpd.readArrayLong(objectSize)
        val cols = fpd.readArrayInt(objectSize)
        val values = fpd.readArrayLong(objectSize)
        PushMatrixLong(id, rows, cols, values)

      case SerializationConstants.pushVectorDoubleByte =>
        val id = fpd.readInt()
        val keys = fpd.readArrayLong(objectSize)
        val values = fpd.readArrayDouble(objectSize)
        PushVectorDouble(id, keys, values)

      case SerializationConstants.pushVectorFloatByte =>
        val id = fpd.readInt()
        val keys = fpd.readArrayLong(objectSize)
        val values = fpd.readArrayFloat(objectSize)
        PushVectorFloat(id, keys, values)

      case SerializationConstants.pushVectorIntByte =>
        val id = fpd.readInt()
        val keys = fpd.readArrayLong(objectSize)
        val values = fpd.readArrayInt(objectSize)
        PushVectorInt(id, keys, values)

      case SerializationConstants.pushVectorLongByte =>
        val id = fpd.readInt()
        val keys = fpd.readArrayLong(objectSize)
        val values = fpd.readArrayLong(objectSize)
        PushVectorLong(id, keys, values)
    }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case x: PullMatrix =>
        val fps = new FastPrimitiveSerializer(5 + x.rows.length * SerializationConstants.sizeOfLong +
          x.rows.length * SerializationConstants.sizeOfInt)
        fps.writeByte(SerializationConstants.pullMatrixByte)
        fps.writeInt(x.rows.length)
        fps.writeArrayLong(x.rows)
        fps.writeArrayInt(x.cols)
        fps.bytes

      case x: PullMatrixRows =>
        val fps = new FastPrimitiveSerializer(5 + x.rows.length * SerializationConstants.sizeOfLong)
        fps.writeByte(SerializationConstants.pullMatrixRowsByte)
        fps.writeInt(x.rows.length)
        fps.writeArrayLong(x.rows)
        fps.bytes

      case x: PullVector =>
        val fps = new FastPrimitiveSerializer(5 + x.keys.length * SerializationConstants.sizeOfLong)
        fps.writeByte(SerializationConstants.pullVectorByte)
        fps.writeInt(x.keys.length)
        fps.writeArrayLong(x.keys)
        fps.bytes

      case x: PushMatrixDouble =>
        val fps = new FastPrimitiveSerializer(9 + x.rows.length * SerializationConstants.sizeOfLong +
          x.rows.length * SerializationConstants.sizeOfInt +
          x.rows.length * SerializationConstants.sizeOfDouble)
        fps.writeByte(SerializationConstants.pushMatrixDoubleByte)
        fps.writeInt(x.rows.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.rows)
        fps.writeArrayInt(x.cols)
        fps.writeArrayDouble(x.values)
        fps.bytes

      case x: PushMatrixFloat =>
        val fps = new FastPrimitiveSerializer(9 + x.rows.length * SerializationConstants.sizeOfLong +
          x.rows.length * SerializationConstants.sizeOfInt +
          x.rows.length * SerializationConstants.sizeOfFloat)
        fps.writeByte(SerializationConstants.pushMatrixFloatByte)
        fps.writeInt(x.rows.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.rows)
        fps.writeArrayInt(x.cols)
        fps.writeArrayFloat(x.values)
        fps.bytes

      case x: PushMatrixInt =>
        val fps = new FastPrimitiveSerializer(9 + x.rows.length * SerializationConstants.sizeOfLong +
          x.rows.length * SerializationConstants.sizeOfInt +
          x.rows.length * SerializationConstants.sizeOfInt)
        fps.writeByte(SerializationConstants.pushMatrixIntByte)
        fps.writeInt(x.rows.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.rows)
        fps.writeArrayInt(x.cols)
        fps.writeArrayInt(x.values)
        fps.bytes

      case x: PushMatrixLong =>
        val fps = new FastPrimitiveSerializer(9 + x.rows.length * SerializationConstants.sizeOfLong +
          x.rows.length * SerializationConstants.sizeOfInt +
          x.rows.length * SerializationConstants.sizeOfLong)
        fps.writeByte(SerializationConstants.pushMatrixLongByte)
        fps.writeInt(x.rows.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.rows)
        fps.writeArrayInt(x.cols)
        fps.writeArrayLong(x.values)
        fps.bytes

      case x: PushVectorDouble =>
        val fps = new FastPrimitiveSerializer(9 + x.keys.length * SerializationConstants.sizeOfLong +
          x.keys.length * SerializationConstants.sizeOfDouble)
        fps.writeByte(SerializationConstants.pushVectorDoubleByte)
        fps.writeInt(x.keys.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.keys)
        fps.writeArrayDouble(x.values)
        fps.bytes

      case x: PushVectorFloat =>
        val fps = new FastPrimitiveSerializer(9 + x.keys.length * SerializationConstants.sizeOfLong +
          x.keys.length * SerializationConstants.sizeOfFloat)
        fps.writeByte(SerializationConstants.pushVectorFloatByte)
        fps.writeInt(x.keys.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.keys)
        fps.writeArrayFloat(x.values)
        fps.bytes

      case x: PushVectorInt =>
        val fps = new FastPrimitiveSerializer(9 + x.keys.length * SerializationConstants.sizeOfLong +
          x.keys.length * SerializationConstants.sizeOfInt)
        fps.writeByte(SerializationConstants.pushVectorIntByte)
        fps.writeInt(x.keys.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.keys)
        fps.writeArrayInt(x.values)
        fps.bytes

      case x: PushVectorLong =>
        val fps = new FastPrimitiveSerializer(9 + x.keys.length * SerializationConstants.sizeOfLong +
          x.keys.length * SerializationConstants.sizeOfLong)
        fps.writeByte(SerializationConstants.pushVectorLongByte)
        fps.writeInt(x.keys.length)
        fps.writeInt(x.id)
        fps.writeArrayLong(x.keys)
        fps.writeArrayLong(x.values)
        fps.bytes
    }
  }
}
