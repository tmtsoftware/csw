package csw.services.event.internal.redis

import java.nio.ByteBuffer

import csw.messages.events.{Event, EventKey}
import csw.messages.params.pb.{PbConverter, TypeMapperSupport}
import csw_protobuf.events.PbEvent
import romaine.codec.{RomaineByteCodec, RomaineStringCodec}

import scala.util.control.NonFatal

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
object EventRomaineCodecs {
  implicit object EventKeyRomaineCodec extends RomaineStringCodec[EventKey] {
    override def toString(eventKey: EventKey): String = eventKey.key
    override def fromString(str: String): EventKey    = EventKey(str)
  }

  implicit object EventRomaineCodec extends RomaineByteCodec[Event] {
    override def toBytes(event: Event): ByteBuffer = {
      val pbEvent = PbConverter.toPbEvent(event)
      ByteBuffer.wrap(pbEvent.toByteArray)
    }
    override def fromBytes(byteBuffer: ByteBuffer): Event = {
      try {
        val bytes = new Array[Byte](byteBuffer.remaining)
        byteBuffer.get(bytes)
        PbConverter.fromPbEvent(PbEvent.parseFrom(bytes))
      } catch {
        case NonFatal(_) ⇒ Event.badEvent()
      }
    }
  }

}
