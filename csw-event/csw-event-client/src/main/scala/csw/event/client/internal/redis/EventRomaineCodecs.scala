package csw.event.client.internal.redis

import java.nio.ByteBuffer

import csw.event.client.internal.commons.EventConverter
import csw.params.events.{Event, EventKey}
import romaine.codec.{RomaineByteCodec, RomaineStringCodec}

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
private[event] object EventRomaineCodecs {

  implicit object EventKeyRomaineCodec extends RomaineStringCodec[EventKey] {
    override def toString(eventKey: EventKey): String = eventKey.key
    override def fromString(str: String): EventKey    = EventKey(str)
  }

  implicit object EventRomaineCodec extends RomaineByteCodec[Event] {
    override def toBytes(event: Event): ByteBuffer        = EventConverter.toBytes[ByteBuffer](event)
    override def fromBytes(byteBuffer: ByteBuffer): Event = EventConverter.toEvent(byteBuffer)
  }
}
