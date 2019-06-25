package csw.event.client.internal.redis

import java.nio.ByteBuffer

import csw.event.client.internal.commons.EventConverter
import csw.params.events.{Event, EventKey}
import romaine.codec.RomaineByteCodec

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
private[event] object EventRomaineCodecs {

  implicit val eventKeyRomaineCodec: RomaineByteCodec[EventKey] = RomaineByteCodec.viaString(_.key, EventKey.apply)

  implicit object EventRomaineCodec extends RomaineByteCodec[Event] {
    override def toBytes(event: Event): ByteBuffer        = EventConverter.toBytes[ByteBuffer](event)
    override def fromBytes(byteBuffer: ByteBuffer): Event = EventConverter.toEvent(byteBuffer)
  }
}
