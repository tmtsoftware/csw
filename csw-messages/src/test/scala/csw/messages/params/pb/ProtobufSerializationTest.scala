package csw.messages.params.pb

import java.time.Instant
import java.util.UUID

import csw.messages.ccs.events._
import csw.messages.params.generics.KeyType
import csw.messages.params.generics.KeyType.{ChoiceKey, RaDecKey, StructKey}
import csw.messages.params.models.Units.{arcmin, joule}
import csw.messages.params.models._
import csw_protobuf.events.PbEvent
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

// DEOPSCSW-297: Merge protobuf branch in master
class ProtobufSerializationTest extends FunSpec with Matchers with BeforeAndAfterAll {

  describe("Test protobuf serialization of Events") {

    it("should serialize StatusEvent") {
      val eventInfo = EventInfo("wfos.blue.filter")
      val raDecKey  = RaDecKey.make("raDecKey")

      val raDec1 = RaDec(10.20, 40.20)
      val raDec2 = RaDec(100.20, 400.20)
      val param  = raDecKey.set(raDec1, raDec2).withUnits(arcmin)

      val statusEvent: StatusEvent = StatusEvent(eventInfo).add(param)

      //able to generate protobuf from event
      StatusEvent.fromPb(statusEvent.toPb) shouldBe statusEvent

      //able to generate event from protobuf byteArray
      StatusEvent.fromPb(PbEvent.parseFrom(statusEvent.toPb.toByteArray)) shouldBe statusEvent
    }

    it("should serialize ObserveEvent") {
      val eventInfo = EventInfo("wfos.blue.filter", Instant.now().minusSeconds(60))

      val jupiter   = Choice("Jupiter")
      val saturn    = Choice("Saturn")
      val pluto     = Choice("Pluto")
      val choiceKey = ChoiceKey.make("choiceKey", jupiter, saturn, pluto)

      val param = choiceKey.set(jupiter, pluto).withUnits(arcmin)

      val observeEvent: ObserveEvent = ObserveEvent(eventInfo).add(param)

      //able to generate protobuf from event
      ObserveEvent.fromPb(observeEvent.toPb) shouldBe observeEvent

      //able to generate event from protobuf byteArray
      ObserveEvent.fromPb(PbEvent.parseFrom(observeEvent.toPb.toByteArray)) shouldBe observeEvent
    }

    it("should serialize SystemEvent") {
      val eventInfo1 = EventInfo("wfos.blue.filter", Instant.now().minusSeconds(60), ObsId("Obs002"))
      val eventInfo2 = EventInfo(
        Prefix("wfos.blue.filter"),
        EventTime(Instant.now().minusSeconds(3600)),
        Some(ObsId("Obs002")),
        UUID.randomUUID().toString()
      )

      val structKey = StructKey.make("structKey")

      val ra     = KeyType.StringKey.make("ra")
      val dec    = KeyType.StringKey.make("dec")
      val epoch  = KeyType.DoubleKey.make("epoch")
      val struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val param = structKey.set(struct).withUnits(joule)

      val systemEvent1: SystemEvent = SystemEvent(eventInfo1).add(param)
      val systemEvent2: SystemEvent = SystemEvent(eventInfo2).add(param)

      //able to generate protobuf from event
      SystemEvent.fromPb(systemEvent1.toPb) shouldBe systemEvent1
      SystemEvent.fromPb(systemEvent2.toPb) shouldBe systemEvent2

      //able to generate event from protobuf byteArray
      SystemEvent.fromPb(PbEvent.parseFrom(systemEvent1.toPb.toByteArray)) shouldBe systemEvent1
      SystemEvent.fromPb(PbEvent.parseFrom(systemEvent2.toPb.toByteArray)) shouldBe systemEvent2
    }
  }
}
