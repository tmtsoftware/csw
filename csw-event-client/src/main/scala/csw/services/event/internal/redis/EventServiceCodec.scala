package csw.services.event.internal.redis

import java.nio.ByteBuffer

import csw.messages.events.{Event, EventKey}
import io.lettuce.core.codec.RedisCodec

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
object EventServiceCodec extends RedisCodec[EventKey, Event] with BaseEventServiceCodec {

  override def encodeKey(eventKey: EventKey): ByteBuffer = super.encodeStringKey(eventKey.key)

  override def decodeKey(byteBuf: ByteBuffer): EventKey = EventKey(super.decodeStringKey(byteBuf))
}
