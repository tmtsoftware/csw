package csw.services.alarm.client.internal

import java.nio.ByteBuffer

import csw.services.alarm.api.internal._
import csw.services.alarm.api.models._
import io.lettuce.core.codec.Utf8StringCodec
import romaine.reactive.RedisKeySpaceCodec
import ujson.Js
import upickle.default._

class AlarmCodec[K: ReadWriter, V: ReadWriter] extends RedisKeySpaceCodec[K, V] {
  private lazy val utf8StringCodec = new Utf8StringCodec()

  override def encodeKey(key: K): ByteBuffer     = utf8StringCodec.encodeKey(toKeyString(key))
  override def decodeKey(byteBuf: ByteBuffer): K = fromKeyString(utf8StringCodec.decodeKey(byteBuf))

  override def encodeValue(value: V): ByteBuffer   = utf8StringCodec.encodeValue(write(value))
  override def decodeValue(byteBuf: ByteBuffer): V = read[V](utf8StringCodec.decodeValue(byteBuf))

  override def toKeyString(key: K): String         = writeJs(key).str
  override def fromKeyString(keyString: String): K = read[K](Js.Str(keyString))
}

object AlarmCodec extends AlarmRW {
  implicit object MetadataCodec extends AlarmCodec[MetadataKey, AlarmMetadata]
  implicit object StatusCodec   extends AlarmCodec[StatusKey, AlarmStatus]
  implicit object SeverityCodec extends AlarmCodec[SeverityKey, AlarmSeverity]
  implicit object StringCodec   extends AlarmCodec[String, String]
}
