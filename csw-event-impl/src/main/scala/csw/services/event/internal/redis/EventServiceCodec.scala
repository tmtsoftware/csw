package csw.services.event.internal.redis

import java.nio.ByteBuffer

import akka.util.ByteString
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec

object EventServiceCodec extends RedisCodec[String, PbEvent] {
  override def encodeKey(key: String): ByteBuffer   = ByteString(key).asByteBuffer
  override def decodeKey(bytes: ByteBuffer): String = ByteString(bytes).utf8String

  override def encodeValue(value: PbEvent): ByteBuffer   = ByteString(value.toByteArray).asByteBuffer
  override def decodeValue(byteBuf: ByteBuffer): PbEvent = PbEvent.parseFrom(ByteString(byteBuf).toArray)
}
