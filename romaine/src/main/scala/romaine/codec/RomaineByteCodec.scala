package romaine.codec
import java.nio.ByteBuffer

import io.lettuce.core.codec.Utf8StringCodec

trait RomaineByteCodec[T] {
  def toBytes(x: T): ByteBuffer
  def fromBytes(byteBuffer: ByteBuffer): T
}

object RomaineByteCodec {
  def apply[T](implicit x: RomaineByteCodec[T]): RomaineByteCodec[T] = x

  private lazy val utf8StringCodec = new Utf8StringCodec()
  implicit def viaStringCodec[T: RomaineStringCodec]: RomaineByteCodec[T] = new RomaineByteCodec[T] {
    override def toBytes(x: T): ByteBuffer            = utf8StringCodec.encodeKey(RomaineStringCodec[T].toString(x))
    override def fromBytes(byteBuffer: ByteBuffer): T = RomaineStringCodec[T].fromString(utf8StringCodec.decodeKey(byteBuffer))
  }
}
