package glint.serialization

import java.nio.ByteBuffer

import akka.serialization.{ByteBufferSerializer, SerializerWithStringManifest}

abstract class GlintSerializer extends SerializerWithStringManifest with ByteBufferSerializer {

  override def toBinary(o: AnyRef): Array[Byte] = {
    val buf = ByteBuffer.allocate(1024*1024)
    toBinary(o, buf)
    buf.flip()
    val bytes = new Array[Byte](buf.remaining)
    buf.get(bytes)
    bytes
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    fromBinary(ByteBuffer.wrap(bytes), manifest)
  }

  override def manifest(o: AnyRef): String = "?"

}
