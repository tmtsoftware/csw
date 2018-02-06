package csw.services.event.impl

import java.nio.ByteBuffer

import akka.util.ByteString
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec

object EventServiceCodec extends RedisCodec[String, PbEvent] {

  override def decodeKey(bytes: ByteBuffer): String = ByteString(bytes).utf8String

  override def encodeKey(key: String): ByteBuffer = ByteString(key).asByteBuffer

  override def encodeValue(value: PbEvent): ByteBuffer = ByteBuffer.wrap(value.toByteArray)

  override def decodeValue(bytes: ByteBuffer): PbEvent = PbEvent.parseFrom(bytes.array())

}
