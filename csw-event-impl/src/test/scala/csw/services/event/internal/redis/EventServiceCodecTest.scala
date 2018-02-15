package csw.services.event.internal.redis

import java.nio.ByteBuffer

import akka.util.ByteString
import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import org.scalatest.{FunSuite, Matchers}

class EventServiceCodecTest extends FunSuite with Matchers {
  val prefix    = Prefix("test.prefix")
  val eventName = EventName("system")
  val event     = SystemEvent(prefix, eventName)
  val pbEvent   = Event.typeMapper.toBase(event)

  test("testDecodeKey") {
    val byteBuf = ByteString("test").asByteBuffer
    EventServiceCodec.decodeKey(byteBuf) shouldBe "test"
  }

  test("testEncodeKey") {
    val byteBuf = ByteString("test").asByteBuffer
    EventServiceCodec.encodeKey("test") shouldBe byteBuf
  }

  test("testEncodeValue") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    EventServiceCodec.encodeValue(pbEvent) shouldBe byteBuf
  }

  test("testDecodeValue") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    EventServiceCodec.decodeValue(byteBuf) shouldBe pbEvent
  }
}
