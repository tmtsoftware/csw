package csw.event.client.internal.redis

import java.nio.ByteBuffer

import com.typesafe.config.ConfigFactory
import csw.event.client.pb.PbConverter
import csw.params.events.{Event, EventKey}
import csw_protobuf.events.PbEvent
import io.bullet.borer.Cbor
import romaine.codec.{RomaineByteCodec, RomaineStringCodec}

import scala.util.control.NonFatal

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
private[event] object EventRomaineCodecs {
  import csw.params.core.formats.CborSupport._

  implicit object EventKeyRomaineCodec extends RomaineStringCodec[EventKey] {
    override def toString(eventKey: EventKey): String = eventKey.key
    override def fromString(str: String): EventKey    = EventKey(str)
  }

  private lazy val isProtobufOn: Boolean = ConfigFactory.load().getBoolean("csw-event.protobuf-serialization")

  implicit lazy val EventRomaineCodec: RomaineByteCodec[Event] = if (isProtobufOn) EventRomainePbCodec else EventRomaineCborCodec

  object EventRomainePbCodec extends RomaineByteCodec[Event] {
    println("****************** Using Pb Codec *********************")
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

  object EventRomaineCborCodec extends RomaineByteCodec[Event] {
    println("****************** Using Cbor Codec *********************")
    override def toBytes(event: Event): ByteBuffer = {
      ByteBuffer.wrap(Cbor.encode(event).toByteArray)
    }
    override def fromBytes(byteBuffer: ByteBuffer): Event = {
      try {
        val bytes = new Array[Byte](byteBuffer.remaining)
        byteBuffer.get(bytes)
        Cbor.decode(bytes).to[Event].value
      } catch {
        case NonFatal(_) ⇒ Event.badEvent()
      }
    }
  }

}
