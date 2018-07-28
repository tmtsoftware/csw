package csw.services.event.internal.redis

import java.nio.ByteBuffer

import csw.messages.events.Event
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.Utf8StringCodec

import scala.util.control.NonFatal

trait BaseEventServiceCodec {

  private lazy val utf8StringCodec = new Utf8StringCodec()

  def encodeStringKey(key: String): ByteBuffer = utf8StringCodec.encodeKey(key)

  def decodeStringKey(byteBuf: ByteBuffer): String = utf8StringCodec.decodeKey(byteBuf)

  def encodeValue(event: Event): ByteBuffer = {
    val pbEvent = Event.typeMapper.toBase(event)
    ByteBuffer.wrap(pbEvent.toByteArray)
  }

  def decodeValue(byteBuf: ByteBuffer): Event =
    try {
      val bytes = new Array[Byte](byteBuf.remaining)
      byteBuf.get(bytes)
      Event.fromPb(PbEvent.parseFrom(bytes))
    } catch {
      case NonFatal(_) ⇒ Event.badEvent()
    }
}
