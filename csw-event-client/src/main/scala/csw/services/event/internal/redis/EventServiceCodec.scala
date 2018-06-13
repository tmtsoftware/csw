package csw.services.event.internal.redis

import java.nio.ByteBuffer

import csw.messages.events.{Event, EventKey}
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.{RedisCodec, Utf8StringCodec}

object EventServiceCodec extends RedisCodec[EventKey, Event] {

  private lazy val utf8StringCodec = new Utf8StringCodec()

  override def encodeKey(eventKey: EventKey): ByteBuffer = utf8StringCodec.encodeKey(eventKey.key)

  override def decodeKey(byteBuf: ByteBuffer): EventKey = EventKey(utf8StringCodec.decodeKey(byteBuf))

  override def encodeValue(event: Event): ByteBuffer = {
    val pbEvent = Event.typeMapper.toBase(event)
    ByteBuffer.wrap(pbEvent.toByteArray)
  }

  override def decodeValue(byteBuf: ByteBuffer): Event = {
    val bytes = new Array[Byte](byteBuf.remaining)
    byteBuf.get(bytes)
    val pbEvent = PbEvent.parseFrom(bytes)
    Event.fromPb(pbEvent)
  }
}
