package csw.services.event.internal.redis

import java.nio.ByteBuffer

import akka.util.ByteString
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import org.scalatest.{FunSuite, Matchers}

class PatternBasedEventServiceCodecTest extends FunSuite with Matchers {
  private val prefix    = Prefix("test.prefix")
  private val eventName = EventName("system")
  private val event     = SystemEvent(prefix, eventName)
  private val pbEvent   = Event.typeMapper.toBase(event)

  test("event key string received can be decoded into equivalent string") {
    val byteBuf = ByteString("testKey").asByteBuffer
    PatternBasedEventServiceCodec.decodeKey(byteBuf) shouldBe "testKey"
  }

  // DEOPSCSW-334 : Publish an event
  test("event key string is encoded as bytes") {
    val byteBuf = ByteString("testPrefix.testName").asByteBuffer
    PatternBasedEventServiceCodec.encodeKey("testPrefix.testName") shouldBe byteBuf
  }

  // DEOPSCSW-334 : Publish an event
  test("event value is sent as encoded equivalent protobuf value") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    PatternBasedEventServiceCodec.encodeValue(event) shouldBe byteBuf
  }

  test("event value is decoded from received encoded equivalent protobuf") {
    val byteBuf = ByteBuffer.wrap(pbEvent.toByteArray)
    PatternBasedEventServiceCodec.decodeValue(byteBuf) shouldBe event
  }
}
