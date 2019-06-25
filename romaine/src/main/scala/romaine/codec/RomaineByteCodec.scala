package romaine.codec

import java.nio.ByteBuffer

import io.lettuce.core.codec.Utf8StringCodec

trait RomaineByteCodec[T] {
  def toBytes(x: T): ByteBuffer
  def fromBytes(byteBuffer: ByteBuffer): T
}

object RomaineByteCodec {

  implicit class FromBytes(val bytes: ByteBuffer) extends AnyVal {
    def as[A: RomaineByteCodec]: A = implicitly[RomaineByteCodec[A]].fromBytes(bytes)
  }

  implicit class FromString(val string: String) extends AnyVal {
    def as[A: RomaineByteCodec]: A = utf8StringCodec.encodeValue(string).as[A]
  }

  implicit class ToBytesAndString[A](val x: A) extends AnyVal {
    def asBytes(implicit c: RomaineByteCodec[A]): ByteBuffer = c.toBytes(x)
    def asString(implicit c: RomaineByteCodec[A]): String    = utf8StringCodec.decodeValue(x.asBytes)
  }

  private lazy val utf8StringCodec = new Utf8StringCodec()

  implicit val stringRomaineCodec: RomaineByteCodec[String] = viaString(identity, identity)

  def viaString[T](encode: T ⇒ String, decode: String ⇒ T): RomaineByteCodec[T] = new RomaineByteCodec[T] {
    override def toBytes(x: T): ByteBuffer            = utf8StringCodec.encodeValue(encode(x))
    override def fromBytes(byteBuffer: ByteBuffer): T = decode(utf8StringCodec.decodeValue(byteBuffer))
  }
}
