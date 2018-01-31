package csw.messages.params.pb

import csw.messages.ccs.events._
import csw.messages.params.generics.KeyType
import csw.messages.params.generics.KeyType.{ChoiceKey, StructKey}
import csw.messages.params.models.Units.{arcmin, joule}
import csw.messages.params.models._
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

// DEOPSCSW-297: Merge protobuf branch in master
class ProtobufSerializationTest extends FunSpec with Matchers with BeforeAndAfterAll {

  describe("Test protobuf serialization of Events") {
    val prefix: Prefix = Prefix("wfos.blue.filter")
    val eventName      = "filter wheel"

    it("should serialize ObserveEvent") {

      val jupiter = Choice("Jupiter")
      val saturn  = Choice("Saturn")
      val pluto   = Choice("Pluto")

      val choiceKey = ChoiceKey.make("choiceKey", jupiter, saturn, pluto)

      val param                      = choiceKey.set(jupiter, pluto).withUnits(arcmin)
      val observeEvent: ObserveEvent = ObserveEvent(prefix, eventName).add(param)

      //able to generate protobuf from event
      ObserveEvent.fromPb(observeEvent.toPb) shouldBe observeEvent

      //able to generate event from protobuf byteArray
      ObserveEvent.fromPb(observeEvent.toPb) shouldBe observeEvent
    }

    it("should serialize SystemEvent") {
      val structKey = StructKey.make("structKey")

      val ra     = KeyType.StringKey.make("ra")
      val dec    = KeyType.StringKey.make("dec")
      val epoch  = KeyType.DoubleKey.make("epoch")
      val struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val param = structKey.set(struct).withUnits(joule)

      val systemEvent1: SystemEvent = SystemEvent(prefix, eventName).add(param)

      //able to generate protobuf from event
      SystemEvent.fromPb(systemEvent1.toPb) shouldBe systemEvent1
    }
  }
}
