package csw.services.event.internal.redis

import java.nio.ByteBuffer
import java.nio.charset.Charset

import csw.messages.events.{Event, EventKey}
import csw_protobuf.events.PbEvent
import io.lettuce.core.codec.RedisCodec

object EventServiceCodec extends RedisCodec[EventKey, Event] {

  private val utf8Charset: Charset = Charset.forName("utf8")

  override def encodeKey(eventKey: EventKey): ByteBuffer =
    utf8Charset.encode(eventKey.key)

  override def decodeKey(byteBuf: ByteBuffer): EventKey =
    EventKey(utf8Charset.decode(byteBuf).toString)

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

object PatternBasedEventServiceCodec extends RedisCodec[String, Event] {

  private val utf8Charset: Charset = Charset.forName("utf8")

  override def encodeKey(eventKey: String): ByteBuffer =
    utf8Charset.encode(eventKey)

  override def decodeKey(byteBuf: ByteBuffer): String =
    utf8Charset.decode(byteBuf).toString

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
