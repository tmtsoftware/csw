package csw.services.event.internal.redis

import java.nio.ByteBuffer

import akka.util.ByteString
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import org.scalatest.{FunSuite, Matchers}

class EventServiceCodecTest extends FunSuite with Matchers {
  val prefix    = Prefix("test.prefix")
  val eventName = EventName("system")
  val event     = SystemEvent(prefix, eventName)
  val pbEvent   = Event.typeMapper.toBase(event)

  test("testDecodeKey") {
    val byteBuf = ByteString("testKey").asByteBuffer
    EventServiceCodec.decodeKey(byteBuf) shouldBe EventKey("testKey")
  }

  test("testEncodeKey") {
    val byteBuf = ByteString("testKey").asByteBuffer
    EventServiceCodec.encodeKey(EventKey("testKey")) shouldBe byteBuf
  }

  test("testEncodeValue") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    EventServiceCodec.encodeValue(event) shouldBe byteBuf
  }

  test("testDecodeValue") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    EventServiceCodec.decodeValue(byteBuf) shouldBe event
  }
}
