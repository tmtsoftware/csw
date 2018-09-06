package romaine.codec

import java.nio.ByteBuffer

import io.lettuce.core.codec.Utf8StringCodec
import romaine.codec.RomaineStringCodec.{FromString, ToString}

trait RomaineByteCodec[T] {
  def toBytes(x: T): ByteBuffer
  def fromBytes(byteBuffer: ByteBuffer): T
}

object RomaineByteCodec {

  implicit class FromBytes(val bytes: ByteBuffer) extends AnyVal {
    def as[A: RomaineByteCodec]: A = implicitly[RomaineByteCodec[A]].fromBytes(bytes)
  }

  implicit class ToBytes[A](val x: A) extends AnyVal {
    def asBytes(implicit c: RomaineByteCodec[A]): ByteBuffer = c.toBytes(x)
  }

  private lazy val utf8StringCodec = new Utf8StringCodec()

  implicit def viaStringCodec[T: RomaineStringCodec]: RomaineByteCodec[T] = new RomaineByteCodec[T] {
    override def toBytes(x: T): ByteBuffer            = utf8StringCodec.encodeKey(x.asString)
    override def fromBytes(byteBuffer: ByteBuffer): T = utf8StringCodec.decodeKey(byteBuffer).as[T]
  }
}
