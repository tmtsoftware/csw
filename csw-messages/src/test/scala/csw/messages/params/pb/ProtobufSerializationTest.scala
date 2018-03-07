package csw.messages.params.pb

import csw.messages.events._
import csw.messages.params.generics.KeyType
import csw.messages.params.generics.KeyType.{ChoiceKey, StructKey}
import csw.messages.params.models.Units.{arcmin, joule}
import csw.messages.params.models._
import csw_protobuf.events.PbEvent
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

// DEOPSCSW-297: Merge protobuf branch in master
// DEOPSCSW-327: Define Event Data Structure
// DEOPSCSW-328: Basic information of Event needed for routing and Diagnostic use
class ProtobufSerializationTest extends FunSpec with Matchers with BeforeAndAfterAll {

  describe("Test protobuf serialization of Events") {
    val prefix: Prefix = Prefix("wfos.blue.filter")
    val eventName      = EventName("filter wheel")

    it("should serialize ObserveEvent") {

      val jupiter = Choice("Jupiter")
      val saturn  = Choice("Saturn")
      val pluto   = Choice("Pluto")

      val choiceKey = ChoiceKey.make("choiceKey", jupiter, saturn, pluto)

      val param                      = choiceKey.set(jupiter, pluto).withUnits(arcmin)
      val observeEvent: ObserveEvent = ObserveEvent(prefix, eventName).add(param)

      //able to generate protobuf from event
      ObserveEvent.fromPb(observeEvent.toPb) shouldBe observeEvent

      Event.fromPb(PbEvent.parseFrom(observeEvent.toPb.toByteArray)) shouldBe a[ObserveEvent]

      //able to generate event from protobuf byteArray
      Event.fromPb(PbEvent.parseFrom(observeEvent.toPb.toByteArray)) shouldBe observeEvent
    }

    it("should serialize SystemEvent") {
      val structKey = StructKey.make("structKey")

      val ra     = KeyType.StringKey.make("ra")
      val dec    = KeyType.StringKey.make("dec")
      val epoch  = KeyType.DoubleKey.make("epoch")
      val struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val param = structKey.set(struct).withUnits(joule)

      val systemEvent: SystemEvent = SystemEvent(prefix, eventName).add(param)

      //able to generate protobuf from event
      SystemEvent.fromPb(systemEvent.toPb) shouldBe systemEvent

      Event.fromPb(PbEvent.parseFrom(systemEvent.toPb.toByteArray)) shouldBe a[SystemEvent]

      //able to generate event from protobuf byteArray
      Event.fromPb(PbEvent.parseFrom(systemEvent.toPb.toByteArray)) shouldBe systemEvent
    }
  }
}
