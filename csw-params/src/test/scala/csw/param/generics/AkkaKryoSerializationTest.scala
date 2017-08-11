package csw.param.generics

import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import com.twitter.chill.akka.AkkaSerializer
import csw.param.commands._
import csw.param.events.{EventInfo, ObserveEvent, StatusEvent, SystemEvent}
import csw.param.generics.KeyType.{ByteArrayKey, ChoiceKey, DoubleMatrixKey, IntKey, RaDecKey, StructKey}
import csw.param.models._
import csw.param.states.{CurrentState, DemandState}
import csw.units.Units.{arcmin, coulomb, encoder, joule, lightyear, meter, pascal, NoUnits}
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class AkkaKryoSerializationTest extends FunSpec with Matchers with BeforeAndAfterAll {
  private final val system        = ActorSystem("example")
  private final val serialization = SerializationExtension(system)
  private final val prefixStr     = "wfos.prog.cloudcover"
  private final val commandInfo   = "Obs001"

  override protected def afterAll(): Unit = Await.result(system.terminate(), 2.seconds)

  describe("Test akka serialization of Commands") {

    it("should serialize Setup") {
      val intKey = IntKey.make("intKey")
      val param  = intKey.set(1, 2, 3).withUnits(coulomb)

      val setup           = Setup(commandInfo, Prefix(prefixStr)).add(param)
      val setupSerializer = serialization.findSerializerFor(setup)

      setupSerializer.getClass shouldBe classOf[AkkaSerializer]

      val setupBytes: Array[Byte] = setupSerializer.toBinary(setup)
      setupSerializer.fromBinary(setupBytes) shouldBe setup
    }

    it("should serialize Observe") {
      val keyName                        = "imageKey"
      val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make(keyName)

      val imgPath  = Paths.get(getClass.getResource("/smallBinary.bin").getPath)
      val imgBytes = Files.readAllBytes(imgPath)

      val binaryImgData: ArrayData[Byte]    = ArrayData.fromArray(imgBytes)
      val param: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits pascal

      val observe           = Observe(commandInfo, Prefix(prefixStr)).add(param)
      val observeSerializer = serialization.findSerializerFor(observe)

      observeSerializer.getClass shouldBe classOf[AkkaSerializer]

      val observeBytes: Array[Byte] = observeSerializer.toBinary(observe)
      observeSerializer.fromBinary(observeBytes) shouldBe observe
    }

    it("should serialize Wait") {
      val keyName                  = "matrixKey"
      val m1: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
      val m2: Array[Array[Double]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

      val matrixData1     = MatrixData.fromArrays(m1)
      val matrixData2     = MatrixData.fromArrays(m2)
      val doubleMatrixKey = DoubleMatrixKey.make(keyName)
      val param           = doubleMatrixKey.set(Array(matrixData1, matrixData2), lightyear)

      val wait: Wait     = Wait(commandInfo, Prefix(prefixStr)).add(param)
      val waitSerializer = serialization.findSerializerFor(wait)

      waitSerializer.getClass shouldBe classOf[AkkaSerializer]

      val waitBytes: Array[Byte] = waitSerializer.toBinary(wait)
      waitSerializer.fromBinary(waitBytes) shouldBe wait
    }
  }

  describe("Test akka serialization of Events") {
    val eventInfo = EventInfo(prefixStr)

    it("should serialize StatusEvent") {
      val raDecKey = RaDecKey.make("raDecKey")

      val raDec1 = RaDec(10.20, 40.20)
      val raDec2 = RaDec(100.20, 400.20)
      val param  = raDecKey.set(raDec1, raDec2).withUnits(arcmin)

      val statusEvent: StatusEvent = StatusEvent(eventInfo).add(param)
      val statusEventSerializer    = serialization.findSerializerFor(statusEvent)

      statusEventSerializer.getClass shouldBe classOf[AkkaSerializer]

      val statusEventBytes: Array[Byte] = statusEventSerializer.toBinary(statusEvent)
      statusEventSerializer.fromBinary(statusEventBytes) shouldBe statusEvent
    }

    it("should serialize ObserveEvent") {
      val jupiter   = Choice("Jupiter")
      val saturn    = Choice("Saturn")
      val pluto     = Choice("Pluto")
      val choiceKey = ChoiceKey.make("choiceKey", jupiter, saturn, pluto)

      val param = choiceKey.set(jupiter, pluto).withUnits(arcmin)

      val observeEvent: ObserveEvent = ObserveEvent(eventInfo).add(param)
      val observeEventSerializer     = serialization.findSerializerFor(observeEvent)

      observeEventSerializer.getClass shouldBe classOf[AkkaSerializer]

      val observeEventBytes: Array[Byte] = observeEventSerializer.toBinary(observeEvent)
      observeEventSerializer.fromBinary(observeEventBytes) shouldBe observeEvent
    }

    it("should serialize SystemEvent") {
      val structKey = StructKey.make("structKey")

      val ra     = KeyType.StringKey.make("ra")
      val dec    = KeyType.StringKey.make("dec")
      val epoch  = KeyType.DoubleKey.make("epoch")
      val struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val param = structKey.set(struct).withUnits(joule)

      val systemEvent: SystemEvent = SystemEvent(eventInfo).add(param)
      val systemEventSerializer    = serialization.findSerializerFor(systemEvent)

      systemEventSerializer.getClass shouldBe classOf[AkkaSerializer]

      val systemEventBytes: Array[Byte] = systemEventSerializer.toBinary(systemEvent)
      systemEventSerializer.fromBinary(systemEventBytes) shouldBe systemEvent
    }
  }

  describe("Test akka serialization of StateVariables") {
    val prefix = Prefix(prefixStr)

    it("should serialize CurrentState") {
      val charKey        = KeyType.CharKey.make("charKey")
      val intArrayKey    = KeyType.IntArrayKey.make("intArrayKey")
      val a1: Array[Int] = Array(1, 2, 3, 4, 5)
      val a2: Array[Int] = Array(10, 20, 30, 40, 50)

      val charParam     = charKey.set('A', 'B', 'C').withUnits(encoder)
      val intArrayParam = intArrayKey.set(a1, a2).withUnits(meter)

      val currentState           = CurrentState(prefix).madd(charParam, intArrayParam)
      val currentStateSerializer = serialization.findSerializerFor(currentState)

      currentStateSerializer.getClass shouldBe classOf[AkkaSerializer]

      val currentStateBytes: Array[Byte] = currentStateSerializer.toBinary(currentState)
      currentStateSerializer.fromBinary(currentStateBytes) shouldBe currentState
    }

    it("should serialize DemandState") {
      val charKey    = KeyType.CharKey.make("charKey")
      val intKey     = KeyType.IntKey.make("intKey")
      val booleanKey = KeyType.BooleanKey.make("booleanKey")

      val charParam    = charKey.set('A', 'B', 'C').withUnits(NoUnits)
      val intParam     = intKey.set(1, 2, 3).withUnits(meter)
      val booleanParam = booleanKey.set(true, false)

      val demandState           = DemandState(prefix).madd(charParam, intParam, booleanParam)
      val demandStateSerializer = serialization.findSerializerFor(demandState)

      demandStateSerializer.getClass shouldBe classOf[AkkaSerializer]

      val demandStateBytes: Array[Byte] = demandStateSerializer.toBinary(demandState)
      demandStateSerializer.fromBinary(demandStateBytes) shouldBe demandState
    }
  }
}
