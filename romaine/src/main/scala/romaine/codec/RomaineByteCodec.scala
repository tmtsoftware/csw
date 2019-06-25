package romaine.codec

import java.nio.ByteBuffer

import enumeratum.{Enum, EnumEntry}
import io.lettuce.core.codec.Utf8StringCodec

trait RomaineByteCodec[T] { outer =>
  def toBytes(value: T): ByteBuffer
  def fromBytes(byteBuffer: ByteBuffer): T

  def bimap[S](from: S => T, to: T => S): RomaineByteCodec[S] = new RomaineByteCodec[S] {
    override def toBytes(value: S): ByteBuffer        = outer.toBytes(from(value))
    override def fromBytes(byteBuffer: ByteBuffer): S = to(outer.fromBytes(byteBuffer))
  }
}

object RomaineByteCodec {

  implicit class FromBytes(val bytes: ByteBuffer) extends AnyVal {
    def as[A: RomaineByteCodec]: A = implicitly[RomaineByteCodec[A]].fromBytes(bytes)
  }

  implicit class FromString(val string: String) extends AnyVal {
    def as[A: RomaineByteCodec]: A = stringRomaineCodec.toBytes(string).as[A]
  }

  implicit class ToBytesAndString[A](val x: A) extends AnyVal {
    def asBytes(implicit c: RomaineByteCodec[A]): ByteBuffer = c.toBytes(x)
    def asString(implicit c: RomaineByteCodec[A]): String    = stringRomaineCodec.fromBytes(x.asBytes)
  }

  implicit lazy val byteBufferRomaineCodec: RomaineByteCodec[ByteBuffer] = new RomaineByteCodec[ByteBuffer] {
    override def toBytes(value: ByteBuffer): ByteBuffer        = value
    override def fromBytes(byteBuffer: ByteBuffer): ByteBuffer = byteBuffer
  }

  implicit lazy val stringRomaineCodec: RomaineByteCodec[String] = {
    val utf8StringCodec = new Utf8StringCodec()
    byteBufferRomaineCodec.bimap[String](utf8StringCodec.encodeValue, utf8StringCodec.decodeValue)
  }

  implicit def enumRomainCodec[T <: EnumEntry: Enum]: RomaineByteCodec[T] =
    stringRomaineCodec.bimap[T](_.entryName, implicitly[Enum[T]].withNameInsensitive)
}
