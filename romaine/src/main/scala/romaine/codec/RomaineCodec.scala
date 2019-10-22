package romaine.codec

import java.nio.ByteBuffer

import enumeratum.{Enum, EnumEntry}
import io.lettuce.core.codec.StringCodec

trait RomaineCodec[T] { outer =>
  def toBytes(value: T): ByteBuffer
  def fromBytes(byteBuffer: ByteBuffer): T

  def bimap[S](from: S => T, to: T => S): RomaineCodec[S] = new RomaineCodec[S] {
    override def toBytes(value: S): ByteBuffer        = outer.toBytes(from(value))
    override def fromBytes(byteBuffer: ByteBuffer): S = to(outer.fromBytes(byteBuffer))
  }
}

object RomaineCodec {

  implicit class FromBytes(val bytes: ByteBuffer) extends AnyVal {
    def as[A: RomaineCodec]: A = implicitly[RomaineCodec[A]].fromBytes(bytes)
  }

  implicit class FromString(val string: String) extends AnyVal {
    def as[A: RomaineCodec]: A = stringCodec.toBytes(string).as[A]
  }

  implicit class ToBytesAndString[A](val x: A) extends AnyVal {
    def asBytes(implicit c: RomaineCodec[A]): ByteBuffer = c.toBytes(x)
    def asString(implicit c: RomaineCodec[A]): String    = stringCodec.fromBytes(x.asBytes)
  }

  implicit lazy val byteBufferCodec: RomaineCodec[ByteBuffer] = new RomaineCodec[ByteBuffer] {
    override def toBytes(value: ByteBuffer): ByteBuffer        = value
    override def fromBytes(byteBuffer: ByteBuffer): ByteBuffer = byteBuffer
  }

  implicit lazy val stringCodec: RomaineCodec[String] = {
    byteBufferCodec.bimap[String](StringCodec.UTF8.encodeValue, StringCodec.UTF8.decodeValue)
  }

  implicit def enumRomainCodec[T <: EnumEntry: Enum]: RomaineCodec[T] =
    stringCodec.bimap[T](_.entryName, implicitly[Enum[T]].withNameInsensitive)
}
