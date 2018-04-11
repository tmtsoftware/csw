package csw.services.event.internal.redis

import java.nio.ByteBuffer

import akka.util.ByteString
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import org.scalatest.{FunSuite, Matchers}

class EventServiceCodecTest extends FunSuite with Matchers {
  private val prefix    = Prefix("test.prefix")
  private val eventName = EventName("system")
  private val event     = SystemEvent(prefix, eventName)
  private val pbEvent   = Event.typeMapper.toBase(event)

  test("event key received can be decoded into equivalent string") {
    val byteBuf = ByteString("testKey").asByteBuffer
    EventServiceCodec.decodeKey(byteBuf) shouldBe EventKey("testKey")
  }

  // DEOPSCSW-334 : Publish an event
  test("event key is encoded as bytes from string") {
    val byteBuf = ByteString("testPrefix.testName").asByteBuffer
    EventServiceCodec.encodeKey(EventKey("testPrefix.testName")) shouldBe byteBuf
  }

  // DEOPSCSW-334 : Publish an event
  test("event value is sent as encoded equivalent protobuf value") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    EventServiceCodec.encodeValue(event) shouldBe byteBuf
  }

  test("event value is decoded from received encoded equivalent protobuf") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    EventServiceCodec.decodeValue(byteBuf) shouldBe event
  }
}
