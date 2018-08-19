package csw.services.event.internal.redis

import java.nio.ByteBuffer

import csw.messages.events.{Event, EventKey}
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec
import romaine.codec.{RomaineByteCodec, RomaineStringCodec}

import scala.util.control.NonFatal

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
object EventRomaineCodecs {
  implicit object EventKeyRomaineCodec extends RomaineStringCodec[EventKey] {
    override def toString(x: EventKey): String        = x.key
    override def fromString(string: String): EventKey = EventKey(string)
  }

  implicit object EventRomaineCodec extends RomaineByteCodec[Event] {
    override def toBytes(x: Event): ByteBuffer = {
      val pbEvent = Event.typeMapper.toBase(x)
      ByteBuffer.wrap(pbEvent.toByteArray)
    }
    override def fromBytes(byteBuffer: ByteBuffer): Event = {
      try {
        val bytes = new Array[Byte](byteBuffer.remaining)
        byteBuffer.get(bytes)
        Event.fromPb(PbEvent.parseFrom(bytes))
      } catch {
        case NonFatal(_) ⇒ Event.badEvent()
      }
    }
  }

}
