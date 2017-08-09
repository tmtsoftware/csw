package csw.param.formats

import java.time.Instant

import csw.param.commands.{CommandInfo, Observe, Setup, Wait}
import csw.param.events._
import csw.param.generics.KeyType
import csw.param.generics.KeyType.{LongMatrixKey, StructKey}
import csw.param.models._
import csw.param.states.{CurrentState, DemandState}
import csw.units.Units.{encoder, meters, NoUnits}
import org.scalatest.{FunSpec, Matchers}
import spray.json.pimpString

class JsonContractTest extends FunSpec with Matchers {

  private val prefixStr: String        = "wfos.blue.filter"
  private val prefix: Prefix           = Prefix(prefixStr)
  private val runId: RunId             = RunId("f22dc990-a02c-4d7e-b719-50b167cb7a1e")
  private val obsId: ObsId             = ObsId("Obs001")
  private val commandInfo: CommandInfo = CommandInfo(obsId, runId)
  private val instantStr: String       = "2017-08-09T06:40:00.898Z"
  private val eventId: String          = "7a4cd6ab-6077-476d-a035-6f83be1de42c"
  private val eventTime: EventTime     = EventTime(Instant.parse(instantStr))
  private val eventInfo: EventInfo     = EventInfo(prefix, eventTime, Some(obsId), eventId)

  describe("Test Sequence Commands") {

    it("Should adhere to specified standard Setup json format") {
      val raDecKey   = KeyType.RaDecKey.make("coords")
      val raDec1     = RaDec(7.3, 12.1)
      val raDec2     = RaDec(9.1, 2.9)
      val raDecParam = raDecKey.set(raDec1, raDec2)

      val setup       = Setup(commandInfo, prefix).add(raDecParam)
      val setupToJson = JsonSupport.writeSequenceCommand(setup)

      val expectedSetupJson = scala.io.Source.fromResource("setup_command.json").mkString
      assert(setupToJson.equals(expectedSetupJson.parseJson))
    }

    it("Should adhere to specified standard Observe json format") {
      val k1      = KeyType.IntKey.make("repeat")
      val k2      = KeyType.StringKey.make("expTime")
      val i1      = k1.set(22)
      val i2      = k2.set("11:10")
      val observe = Observe(commandInfo, prefix).add(i1).add(i2)

      val observeToJson = JsonSupport.writeSequenceCommand(observe)

      val expectedObserveJson = scala.io.Source.fromResource("observe_command.json").mkString
      assert(observeToJson.equals(expectedObserveJson.parseJson))
    }

    it("Should adhere to specified standard Wait json format") {
      val k1                   = LongMatrixKey.make("myMatrix")
      val m1: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12))
      val m2: MatrixData[Long] = MatrixData.fromArrays(Array(2, 3, 4), Array(5, 6, 7), Array(8, 9, 10))
      val matrixParam          = k1.set(m1, m2)

      val wait       = Wait(commandInfo, prefix).add(matrixParam)
      val waitToJson = JsonSupport.writeSequenceCommand(wait)

      val expectedWaitJson = scala.io.Source.fromResource("wait_command.json").mkString
      assert(waitToJson.equals(expectedWaitJson.parseJson))
    }
  }

  describe("Test Events") {

    it("Should adhere to specified standard StatusEvent json format") {
      val k1 = KeyType.IntKey.make("encoder")
      val k2 = KeyType.IntKey.make("windspeed")

      val i1                = k1.set(22)
      val i2                = k2.set(44)
      val statusEvent       = StatusEvent(eventInfo).madd(i1, i2)
      val statusEventToJson = JsonSupport.writeEvent(statusEvent)

      val expectedStatusEventJson = scala.io.Source.fromResource("status_event.json").mkString
      statusEventToJson shouldEqual expectedStatusEventJson.parseJson
    }

    it("Should adhere to specified standard ObserveEvent json format") {
      val structKey = StructKey.make("myStruct")

      val ra         = KeyType.StringKey.make("ra")
      val dec        = KeyType.StringKey.make("dec")
      val epoch      = KeyType.DoubleKey.make("epoch")
      val structItem = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val structParam        = structKey.set(structItem)
      val observeEvent       = ObserveEvent(eventInfo).add(structParam)
      val observeEventToJson = JsonSupport.writeEvent(observeEvent)

      val expectedObserveEventJson = scala.io.Source.fromResource("observe_event.json").mkString
      observeEventToJson shouldEqual expectedObserveEventJson.parseJson
    }

    it("Should adhere to specified standard SystemEvent json format") {
      val a1: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)
      val a2: Array[Byte] = Array[Byte](10, 20, 30, 40, 50)

      val arrayDataKey   = KeyType.ByteArrayKey.make("arrayDataKey")
      val arrayDataParam = arrayDataKey.set(ArrayData(a1), ArrayData(a2))

      val systemEvent       = SystemEvent(eventInfo).add(arrayDataParam)
      val systemEventToJson = JsonSupport.writeEvent(systemEvent)

      val expectedSystemEventJson = scala.io.Source.fromResource("system_event.json").mkString
      systemEventToJson shouldEqual expectedSystemEventJson.parseJson
    }
  }

  describe("Test State Variables") {

    it("Should adhere to specified standard CurrentState json format") {
      val charKey        = KeyType.CharKey.make("charKey")
      val intArrayKey    = KeyType.IntArrayKey.make("intArrayKey")
      val a1: Array[Int] = Array(1, 2, 3, 4, 5)
      val a2: Array[Int] = Array(10, 20, 30, 40, 50)

      val charParam     = charKey.set('A', 'B', 'C').withUnits(encoder)
      val intArrayParam = intArrayKey.set(a1, a2).withUnits(meters)

      val currentState       = CurrentState(prefix).madd(charParam, intArrayParam)
      val currentStateToJson = JsonSupport.writeStateVariable(currentState)

      val expectedCurrentStateJson = scala.io.Source.fromResource("current_State.json").mkString
      currentStateToJson shouldBe expectedCurrentStateJson.parseJson
    }

    it("Should adhere to specified standard DemandState json format") {
      val charKey    = KeyType.CharKey.make("charKey")
      val intKey     = KeyType.IntKey.make("intKey")
      val booleanKey = KeyType.BooleanKey.make("booleanKey")

      val charParam    = charKey.set('A', 'B', 'C').withUnits(NoUnits)
      val intParam     = intKey.set(1, 2, 3).withUnits(meters)
      val booleanParam = booleanKey.set(true, false)

      val demandState       = DemandState(prefix).madd(charParam, intParam, booleanParam)
      val demandStateToJson = JsonSupport.writeStateVariable(demandState)

      val expectedDemandStateJson = scala.io.Source.fromResource("demand_state.json").mkString
      demandStateToJson shouldBe expectedDemandStateJson.parseJson
    }
  }

}
