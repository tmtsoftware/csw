/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.formats

import csw.params.commands.{CommandName, Observe, Setup, Wait}
import csw.params.core.generics.KeyType
import csw.params.core.generics.KeyType.LongMatrixKey
import csw.params.core.models.Coords.{SolarSystemCoord, Tag}
import csw.params.core.models.Coords.SolarSystemObject.{Jupiter, Venus}
import csw.params.core.models.Units.{NoUnits, encoder, meter}
import csw.params.core.models.*
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.params.events.*
import csw.params.testdata.ParamSetData
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

import java.time.Instant
import scala.io.Source

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-188: Efficient Serialization to/from JSON
// DEOPSCSW-282: Add a timestamp Key and Parameter
// DEOPSCSW-184: Change configurations - attributes and values
// DEOPSCSW-423: Introduce name for CurrentState and DemandState
// DEOPSCSW-401: Remove implicit apply method from prefix
class JsonContractTest extends AnyFunSpec with Matchers {

  private val prefix: Prefix       = Prefix("wfos.blue.filter")
  private val obsId: ObsId         = ObsId("2020A-001-123")
  private val instantStr: String   = "2017-08-09T06:40:00.898Z"
  private val eventId: Id          = Id("7a4cd6ab-6077-476d-a035-6f83be1de42c")
  private val eventTime: UTCTime   = UTCTime(Instant.parse(instantStr))
  private val eventName: EventName = EventName("filter wheel")

  describe("Test Sequence Commands") {

    it(
      "should adhere to specified standard Setup json format | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401, CSW-147"
    ) {
      val solarSystemKey   = KeyType.SolarSystemCoordKey.make("planets")
      val planet1          = SolarSystemCoord(Tag("planet"), Jupiter)
      val planet2          = SolarSystemCoord(Tag("planet"), Venus)
      val solarSystemParam = solarSystemKey.set(planet1, planet2)

      val setup       = Setup(prefix, CommandName("move"), Some(obsId)).add(solarSystemParam)
      val setupToJson = JsonSupport.writeSequenceCommand(setup)

      val expectedSetupJson =
        Json.parse(
          Source
            .fromResource("json/setup_command.json")
            .mkString
        )

      setupToJson shouldEqual expectedSetupJson
    }

    it(
      "should adhere to specified standard Observe json format | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401"
    ) {
      val k1      = KeyType.IntKey.make("repeat")
      val k2      = KeyType.StringKey.make("expTime")
      val i1      = k1.set(22)
      val i2      = k2.set("11:10")
      val observe = Observe(prefix, CommandName("move"), Some(obsId)).add(i1).add(i2)

      val observeToJson = JsonSupport.writeSequenceCommand(observe)

      val expectedObserveJson =
        Json.parse(Source.fromResource("json/observe_command.json").mkString) // .replace("test-runId", observe.runId.id))

      observeToJson shouldEqual expectedObserveJson
    }

    it(
      "should adhere to specified standard Wait json format | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401"
    ) {
      val k1                   = LongMatrixKey.make("myMatrix")
      val m1: MatrixData[Long] = MatrixData.fromArrays(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12))
      val m2: MatrixData[Long] = MatrixData.fromArrays(Array(2, 3, 4), Array(5, 6, 7), Array(8, 9, 10))
      val matrixParam          = k1.set(m1, m2)

      val wait       = Wait(prefix, CommandName("move"), Some(obsId)).add(matrixParam)
      val waitToJson = JsonSupport.writeSequenceCommand(wait)

      val expectedWaitJson =
        Json.parse(Source.fromResource("json/wait_command.json").mkString) // .replace("test-runId", wait.runId.id))

      waitToJson shouldEqual expectedWaitJson
    }
  }

  describe("Test Events") {

    it(
      "should adhere to specified standard ObserveEvent json format | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401"
    ) {

      val ra        = KeyType.StringKey.make("ra")
      val dec       = KeyType.StringKey.make("dec")
      val epoch     = KeyType.DoubleKey.make("epoch")
      val raItem    = ra.set("12:13:14.1")
      val decItem   = dec.set("32:33:34.4")
      val epochItem = epoch.set(1950.0)

      val observeEvent       = ObserveEvent(eventId, prefix, eventName, eventTime, Set(raItem, decItem, epochItem))
      val observeEventToJson = JsonSupport.writeEvent(observeEvent)

      val expectedObserveEventJson =
        Json.parse(Source.fromResource("json/observe_event.json").mkString)
      observeEventToJson shouldEqual expectedObserveEventJson
    }

    it(
      "should adhere to specified standard SystemEvent json format | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401"
    ) {
      val a1: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)

      val arrayDataKey   = KeyType.ByteArrayKey.make("arrayDataKey")
      val arrayDataParam = arrayDataKey.set(ArrayData.fromArray(a1), ArrayData.fromArrays[Byte](10, 20, 30, 40, 50))

      val systemEvent = SystemEvent(eventId, prefix, eventName, eventTime, Set(arrayDataParam))
//      val systemEvent2      = SystemEvent(prefix, eventName, Set(arrayDataParam)).withEventTime(eventTime).copy(eventId = eventId)
      val systemEventToJson = JsonSupport.writeEvent(systemEvent)
//      val systemEventToJson2 = JsonSupport.writeEvent(systemEvent2)

      val expectedSystemEventJson = Json.parse(Source.fromResource("json/system_event.json").mkString)
      systemEventToJson shouldEqual expectedSystemEventJson
//      systemEventToJson2 shouldEqual expectedSystemEventJson
    }
  }

  describe("Test State Variables") {

    it(
      "should adhere to specified standard CurrentState json format | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401, CSW-152"
    ) {
      val charKey        = KeyType.CharKey.make("charKey")
      val intArrayKey    = KeyType.IntArrayKey.make("intArrayKey")
      val a1: Array[Int] = Array(1, 2, 3, 4, 5)
      val a2: Array[Int] = Array(10, 20, 30, 40, 50)
      val utcTimeKey     = KeyType.UTCTimeKey.make("utcTimeKey")
      val stateName      = StateName("testStateName")

      val charParam     = charKey.set('A', 'B', 'C').withUnits(encoder)
      val intArrayParam = intArrayKey.set(a1, a2).withUnits(meter)
      val utcTimeParam =
        utcTimeKey
          .set(
            UTCTime(Instant.ofEpochMilli(0)),
            UTCTime(Instant.parse("2017-09-04T16:28:00.123456789Z"))
          )

      val currentState       = CurrentState(prefix, stateName).madd(charParam, intArrayParam, utcTimeParam)
      val currentStateToJson = JsonSupport.writeStateVariable(currentState)

      val expectedCurrentStateJson =
        Json.parse(Source.fromResource("json/current_State.json").mkString)
      currentStateToJson shouldBe expectedCurrentStateJson
    }

    it(
      "should adhere to specified standard DemandState json format | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401"
    ) {
      val charKey    = KeyType.CharKey.make("charKey")
      val intKey     = KeyType.IntKey.make("intKey")
      val booleanKey = KeyType.BooleanKey.make("booleanKey")
      val utcTimeKey = KeyType.UTCTimeKey.make("utcTimeKey")
      val stateName  = StateName("testStateName")

      val charParam    = charKey.set('A', 'B', 'C').withUnits(NoUnits)
      val intParam     = intKey.set(1, 2, 3).withUnits(meter)
      val booleanParam = booleanKey.set(true, false)
      val utcTimeParam =
        utcTimeKey.set(
          UTCTime(Instant.ofEpochMilli(0)),
          UTCTime(Instant.parse("2017-09-04T16:28:00.123456789Z"))
        )

      val demandState       = DemandState(prefix, stateName).madd(charParam, intParam, booleanParam, utcTimeParam)
      val demandStateToJson = JsonSupport.writeStateVariable(demandState)

      val expectedDemandStateJson = Json.parse(Source.fromResource("json/demand_state.json").mkString)
      demandStateToJson shouldBe expectedDemandStateJson
    }
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  describe("Exercise all types of keys") {
    it(
      "should able to serialize and deserialize Setup command with all keys to and from json | DEOPSCSW-183, DEOPSCSW-188, DEOPSCSW-282, DEOPSCSW-184, DEOPSCSW-423, DEOPSCSW-401, DEOPSCSW-661, CSW-147"
    ) {
      val setup         = Setup(prefix, CommandName("move"), Some(obsId), ParamSetData.paramSet)
      val setupToJson   = JsonSupport.writeSequenceCommand(setup)
      val originalSetup = JsonSupport.readSequenceCommand[Setup](setupToJson)

      val expectedSetupJson = Source
        .fromResource("json/setup_with_all_keys.json")
        .mkString

      val expectedSetup = JsonSupport.readSequenceCommand[Setup](Json.parse(expectedSetupJson))
      originalSetup shouldBe expectedSetup
    }
  }
}
