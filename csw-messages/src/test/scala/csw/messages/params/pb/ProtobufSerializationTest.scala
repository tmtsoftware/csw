package csw.messages.params.pb

import csw.messages.ccs.events._
import csw.messages.params.generics.KeyType
import csw.messages.params.generics.KeyType.{ChoiceKey, RaDecKey, StructKey}
import csw.messages.params.models.Units.{arcmin, joule}
import csw.messages.params.models._
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

// DEOPSCSW-297: Merge protobuf branch in master
class ProtobufSerializationTest extends FunSpec with Matchers with BeforeAndAfterAll {
  private final val prefixStr     = "wfos.prog.cloudcover"

  describe("Test akka serialization of Events") {
    val eventInfo = EventInfo(prefixStr)

    it("should serialize StatusEvent") {
      val raDecKey = RaDecKey.make("raDecKey")

      val raDec1 = RaDec(10.20, 40.20)
      val raDec2 = RaDec(100.20, 400.20)
      val param  = raDecKey.set(raDec1, raDec2).withUnits(arcmin)

      val statusEvent: StatusEvent = StatusEvent(eventInfo).add(param)
      val mapper = EventType.typeMapper[StatusEvent]

      mapper.toCustom(mapper.toBase(statusEvent)) shouldBe statusEvent
    }

    it("should serialize ObserveEvent") {
      val jupiter   = Choice("Jupiter")
      val saturn    = Choice("Saturn")
      val pluto     = Choice("Pluto")
      val choiceKey = ChoiceKey.make("choiceKey", jupiter, saturn, pluto)

      val param = choiceKey.set(jupiter, pluto).withUnits(arcmin)

      val observeEvent: ObserveEvent = ObserveEvent(eventInfo).add(param)
      val mapper = EventType.typeMapper[ObserveEvent]

      mapper.toCustom(mapper.toBase(observeEvent)) shouldBe observeEvent
    }

    it("should serialize SystemEvent") {
      val structKey = StructKey.make("structKey")

      val ra     = KeyType.StringKey.make("ra")
      val dec    = KeyType.StringKey.make("dec")
      val epoch  = KeyType.DoubleKey.make("epoch")
      val struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val param = structKey.set(struct).withUnits(joule)

      val systemEvent: SystemEvent = SystemEvent(eventInfo).add(param)
      val mapper = EventType.typeMapper[SystemEvent]

      mapper.toCustom(mapper.toBase(systemEvent)) shouldBe systemEvent
    }
  }
}
