package csw.messages.params.formats

import java.time.Instant

import csw.messages.ccs.commands.{Observe, Setup, Wait}
import csw.messages.ccs.events._
import csw.messages.params.generics.KeyType
import csw.messages.params.generics.KeyType.{LongMatrixKey, StructKey}
import csw.messages.params.models.Units.{encoder, meter, second, NoUnits}
import csw.messages.params.models._
import csw.messages.params.states.{CurrentState, DemandState}
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json
import scala.io.Source

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-188: Efficient Serialization to/from JSON
//DEOPSCSW-282: Add a timestamp Key and Parameter
//DEOPSCSW-184: Change configurations - attributes and values
class JsonContractTest extends FunSpec with Matchers {

  private val prefix: Prefix       = "wfos.blue.filter"
  private val obsId: ObsId         = ObsId("Obs001")
  private val instantStr: String   = "2017-08-09T06:40:00.898Z"
  private val eventId: String      = "7a4cd6ab-6077-476d-a035-6f83be1de42c"
  private val eventTime: EventTime = EventTime(Instant.parse(instantStr))
  private val eventInfo: EventInfo = EventInfo(prefix, eventTime, Some(obsId), eventId)

  describe("Test Sequence Commands") {

    it("should adhere to specified standard Setup json format") {
      val raDecKey   = KeyType.RaDecKey.make("coords")
      val raDec1     = RaDec(7.3, 12.1)
      val raDec2     = RaDec(9.1, 2.9)
      val raDecParam = raDecKey.set(raDec1, raDec2)

      val setup       = Setup(obsId, prefix).add(raDecParam)
      val setupToJson = Json.prettyPrint(JsonSupport.writeSequenceCommand(setup))

      val expectedSetupJson = Json.prettyPrint(
        Json.parse(
          Source
            .fromResource("json/setup_command.json")
            .mkString
            .replace("test-runId", setup.runId.id)
        )
      )
      setupToJson shouldEqual expectedSetupJson
    }

    it("should adhere to specified standard Observe json format") {
      val k1      = KeyType.IntKey.make("repeat")
      val k2      = KeyType.StringKey.make("expTime")
      val i1      = k1.set(22)
      val i2      = k2.set("11:10")
      val observe = Observe(obsId, prefix).add(i1).add(i2)

      val observeToJson = Json.prettyPrint(JsonSupport.writeSequenceCommand(observe))

      val expectedObserveJson = Json.prettyPrint(
        Json.parse(Source.fromResource("json/observe_command.json").mkString.replace("test-runId", observe.runId.id))
      )
      observeToJson shouldEqual expectedObserveJson
    }

    it("should adhere to specified standard Wait json format") {
      val k1                   = LongMatrixKey.make("myMatrix")
      val m1: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12))
      val m2: MatrixData[Long] = MatrixData.fromArrays(Array(2, 3, 4), Array(5, 6, 7), Array(8, 9, 10))
      val matrixParam          = k1.set(m1, m2)

      val wait       = Wait(obsId, prefix).add(matrixParam)
      val waitToJson = Json.prettyPrint(JsonSupport.writeSequenceCommand(wait))

      val expectedWaitJson = Json.prettyPrint(
        Json.parse(Source.fromResource("json/wait_command.json").mkString.replace("test-runId", wait.runId.id))
      )
      waitToJson shouldEqual expectedWaitJson
    }
  }

  describe("Test Events") {

    it("should adhere to specified standard StatusEvent json format") {
      val k1 = KeyType.IntKey.make("encoder")
      val k2 = KeyType.IntKey.make("windspeed")

      val i1                = k1.set(22)
      val i2                = k2.set(44)
      val statusEvent       = StatusEvent(eventInfo).madd(i1, i2)
      val statusEventToJson = Json.prettyPrint(JsonSupport.writeEvent(statusEvent))

      val expectedStatusEventJson = Json.prettyPrint(Json.parse(Source.fromResource("json/status_event.json").mkString))
      statusEventToJson shouldEqual expectedStatusEventJson
    }

    it("should adhere to specified standard ObserveEvent json format") {
      val structKey = StructKey.make("myStruct")

      val ra         = KeyType.StringKey.make("ra")
      val dec        = KeyType.StringKey.make("dec")
      val epoch      = KeyType.DoubleKey.make("epoch")
      val structItem = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val structParam        = structKey.set(structItem)
      val observeEvent       = ObserveEvent(eventInfo).add(structParam)
      val observeEventToJson = Json.prettyPrint(JsonSupport.writeEvent(observeEvent))

      val expectedObserveEventJson =
        Json.prettyPrint(Json.parse(Source.fromResource("json/observe_event.json").mkString))
      observeEventToJson shouldEqual expectedObserveEventJson
    }

    it("should adhere to specified standard SystemEvent json format") {
      val a1: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)
      val a2: Array[Byte] = Array[Byte](10, 20, 30, 40, 50)

      val arrayDataKey   = KeyType.ByteArrayKey.make("arrayDataKey")
      val arrayDataParam = arrayDataKey.set(ArrayData(a1), ArrayData(a2))

      val systemEvent       = SystemEvent(eventInfo).add(arrayDataParam)
      val systemEventToJson = Json.prettyPrint(JsonSupport.writeEvent(systemEvent))

      val expectedSystemEventJson = Json.prettyPrint(Json.parse(Source.fromResource("json/system_event.json").mkString))
      systemEventToJson shouldEqual expectedSystemEventJson
    }
  }

  describe("Test State Variables") {

    it("should adhere to specified standard CurrentState json format") {
      val charKey        = KeyType.CharKey.make("charKey")
      val intArrayKey    = KeyType.IntArrayKey.make("intArrayKey")
      val a1: Array[Int] = Array(1, 2, 3, 4, 5)
      val a2: Array[Int] = Array(10, 20, 30, 40, 50)
      val timestampKey   = KeyType.TimestampKey.make("timestampKey")

      val charParam     = charKey.set('A', 'B', 'C').withUnits(encoder)
      val intArrayParam = intArrayKey.set(a1, a2).withUnits(meter)
      val timestampParam =
        timestampKey.set(Instant.ofEpochMilli(0), Instant.parse("2017-09-04T16:28:00.123456789Z")).withUnits(second)

      val currentState       = CurrentState(prefix).madd(charParam, intArrayParam, timestampParam)
      val currentStateToJson = Json.prettyPrint(JsonSupport.writeStateVariable(currentState))

      val expectedCurrentStateJson =
        Json.prettyPrint(Json.parse(Source.fromResource("json/current_State.json").mkString))
      currentStateToJson shouldBe expectedCurrentStateJson
    }

    it("should adhere to specified standard DemandState json format") {
      val charKey      = KeyType.CharKey.make("charKey")
      val intKey       = KeyType.IntKey.make("intKey")
      val booleanKey   = KeyType.BooleanKey.make("booleanKey")
      val timestampKey = KeyType.TimestampKey.make("timestampKey")

      val charParam      = charKey.set('A', 'B', 'C').withUnits(NoUnits)
      val intParam       = intKey.set(1, 2, 3).withUnits(meter)
      val booleanParam   = booleanKey.set(true, false)
      val timestampParam = timestampKey.set(Instant.ofEpochMilli(0), Instant.parse("2017-09-04T16:28:00.123456789Z"))

      val demandState       = DemandState(prefix).madd(charParam, intParam, booleanParam, timestampParam)
      val demandStateToJson = Json.prettyPrint(JsonSupport.writeStateVariable(demandState))

      val expectedDemandStateJson = Json.prettyPrint(Json.parse(Source.fromResource("json/demand_state.json").mkString))
      demandStateToJson shouldBe expectedDemandStateJson
    }
  }

  describe("Exercise all types of keys") {
    import KeyType._
    // Simple Key's
    val p1 = BooleanKey.make("BooleanKey").set(true, false)
    val p2 = ByteKey.make("ByteKey").set(Array[Byte](10, 20))
    val p3 = CharKey.make("CharKey").set('A', 'B')
    val p4 = ShortKey.make("ShortKey").set(Array[Short](30, 40))
    val p5 = LongKey.make("LongKey").set(Array[Long](50, 60))
    val p6 = IntKey.make("IntKey").set(70, 80)
    val p7 = FloatKey.make("FloatKey").set(Array[Float](90, 100))
    val p8 = DoubleKey.make("DoubleKey").set(Array[Double](110, 120))
    val p9 =
      TimestampKey.make("TimestampKey").set(Instant.ofEpochMilli(0), Instant.parse("2017-09-04T19:00:00.123456789Z"))

    // ArrayData Key's
    val p10 = ByteArrayKey.make("ByteArrayKey").set(ArrayData.fromArray(Array[Byte](1, 2)))
    val p11 = ShortArrayKey.make("ShortArrayKey").set(ArrayData.fromArray(Array[Short](3, 4)))
    val p12 = LongArrayKey.make("LongArrayKey").set(ArrayData.fromArray(Array[Long](5, 6)))
    val p13 = IntArrayKey.make("IntArrayKey").set(ArrayData.fromArray(Array(7, 8)))
    val p14 = FloatArrayKey.make("FloatArrayKey").set(ArrayData.fromArray(Array[Float](9, 10)))
    val p15 = DoubleArrayKey.make("DoubleArrayKey").set(ArrayData.fromArray(Array[Double](11, 12)))
    // MatrixData Key's
    val p16 = ByteMatrixKey.make("ByteMatrix").set(MatrixData.fromArrays(Array[Byte](1, 2), Array[Byte](3, 4)))
    val p17 = ShortMatrixKey.make("ShortMatrix").set(MatrixData.fromArrays(Array[Short](4, 5), Array[Short](6, 7)))
    val p18 = LongMatrixKey.make("LongMatrix").set(MatrixData.fromArrays(Array[Long](8, 9), Array[Long](10, 11)))
    val p19 = IntMatrixKey.make("IntMatrix").set(MatrixData.fromArrays(Array(12, 13), Array(14, 15)))
    val p20 =
      FloatMatrixKey.make("FloatMatrix").set(MatrixData.fromArrays(Array[Float](16, 17), Array[Float](18, 19)))
    val p21 =
      DoubleMatrixKey.make("DoubleMatrix").set(MatrixData.fromArrays(Array[Double](20, 21), Array[Double](22, 23)))
    // RaDec Key
    val p22 = RaDecKey.make("RaDecKey").set(RaDec(7.3, 12.1))
    // Choice Key
    val p23 = ChoiceKey.make("ChoiceKey", Choices.from("First", "Second")).set("First", "Second")
    // Struct Key
    val p24 = StructKey.make("StructKey").set(Struct(Set(p1, p2)))
    val p25 = StringKey.make("StringKey").set("Str1", "Str2")

    it("should able to serialize and deserialize Setup command with all keys to and from json") {
      val setup = Setup(obsId, prefix).madd(
        p1,
        p2,
        p3,
        p4,
        p5,
        p6,
        p7,
        p8,
        p9,
        p10,
        p11,
        p12,
        p13,
        p14,
        p15,
        p16,
        p17,
        p18,
        p19,
        p20,
        p21,
        p22,
        p23,
        p24,
        p25
      )

      val setupToJson = Json.prettyPrint(JsonSupport.writeSequenceCommand(setup))

      val expectedSetupJson = Source
        .fromResource("json/setup_with_all_keys.json")
        .mkString
        .replace("test-runId", setup.runId.id)
      setupToJson shouldBe Json.prettyPrint(Json.parse(expectedSetupJson))
    }
  }
}
