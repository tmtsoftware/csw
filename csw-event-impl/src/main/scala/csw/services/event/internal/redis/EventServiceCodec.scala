package csw.services.event.internal.redis

import java.nio.ByteBuffer

import akka.util.ByteString
import csw.messages.events.{Event, EventKey}
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec

object EventServiceCodec extends RedisCodec[EventKey, Event] {

  override def encodeKey(eventKey: EventKey): ByteBuffer = ByteString(eventKey.key).asByteBuffer

  override def decodeKey(bytes: ByteBuffer): EventKey = EventKey(ByteString(bytes).utf8String)

  override def encodeValue(event: Event): ByteBuffer = {
    val pbEvent = Event.typeMapper.toBase(event)
    ByteString(pbEvent.toByteArray).asByteBuffer
  }

  override def decodeValue(byteBuf: ByteBuffer): Event = {
    val pbEvent = PbEvent.parseFrom(ByteString(byteBuf).toArray)
    Event.fromPb(pbEvent)
  }
}
