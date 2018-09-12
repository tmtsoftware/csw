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
      val pbEvent: PbEvent           = PbConverter.toPbEvent(observeEvent)

      //able to generate protobuf from event
      PbConverter.fromPbEvent[ObserveEvent](pbEvent) shouldBe observeEvent

      //able to generate event from protobuf byteArray
      PbConverter.fromPbEvent[ObserveEvent](PbEvent.parseFrom(pbEvent.toByteArray)) shouldBe a[ObserveEvent]
      PbConverter.fromPbEvent[ObserveEvent](PbEvent.parseFrom(pbEvent.toByteArray)) shouldBe observeEvent
    }

    it("should serialize SystemEvent") {
      val structKey = StructKey.make("structKey")

      val ra     = KeyType.StringKey.make("ra")
      val dec    = KeyType.StringKey.make("dec")
      val epoch  = KeyType.DoubleKey.make("epoch")
      val struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val param = structKey.set(struct).withUnits(joule)

      val systemEvent: SystemEvent = SystemEvent(prefix, eventName).add(param)
      val pbEvent: PbEvent         = PbConverter.toPbEvent(systemEvent)

      //able to generate protobuf from event
      PbConverter.fromPbEvent[SystemEvent](pbEvent) shouldBe systemEvent

      //able to generate event from protobuf byteArray
      PbConverter.fromPbEvent[SystemEvent](PbEvent.parseFrom(pbEvent.toByteArray)) shouldBe a[SystemEvent]
      PbConverter.fromPbEvent[SystemEvent](PbEvent.parseFrom(pbEvent.toByteArray)) shouldBe systemEvent
    }
  }
}
