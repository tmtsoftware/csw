package example.params

import csw.params.core.formats.{EventCbor, JsonSupport}
import csw.params.core.generics.KeyType.DoubleMatrixKey
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.Coords.SolarSystemObject.{Jupiter, Venus}
import csw.params.core.models.Coords.{SolarSystemCoord, Tag}
import csw.params.core.models._
import csw.params.events._
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

//DEOPSCSW-331: Event Service Accessible to all CSW component builders
class EventsTest extends AnyFunSpec with Matchers {

  describe("Examples of EventTime") {
    it("should show usage of utility functions") {
      //#eventtime
      val source    = Prefix("iris.filter.wheel")
      val eventName = EventName("temperatures")
      val event     = SystemEvent(source, eventName)

      // accessing eventTime
      val eventTime = event.eventTime
      //#eventtime

      assert(eventName == event.eventName)
      assert(eventTime == event.eventTime)
    }
  }

  // DEOPSCSW-330: Include complex payloads - paramset in Event and ObserveEvent
  describe("Examples of Events") {
    // DEOPSCSW-330: Include complex payloads - paramset in Event and ObserveEvent
    it("should show usages of SystemEvent") {

      //#systemevent
      //keys
      val k1: Key[Int]    = KeyType.IntKey.make("encoder")
      val k2: Key[Int]    = KeyType.IntKey.make("speed")
      val k3: Key[String] = KeyType.StringKey.make("filter")
      val k4: Key[Int]    = KeyType.IntKey.make("notUsed")

      //prefixes
      val ck1   = Prefix("wfos.red.filter")
      val name1 = EventName("filterWheel")
      val ck3   = Prefix("iris.imager.filter")
      val name3 = EventName("status")

      //parameters
      val p1: Parameter[Int]    = k1.set(22)
      val p2: Parameter[Int]    = k2.set(44)
      val p3: Parameter[String] = k3.set("A", "B", "C", "D")

      //Create SystemEvent using madd
      val se1: SystemEvent = SystemEvent(ck1, name1).madd(p1, p2)
      //Create SystemEvent using apply
      val se2: SystemEvent = SystemEvent(ck3, name3, Set(p1, p2))
      //Create SystemEvent and use add
      val se3: SystemEvent = SystemEvent(ck3, name3).add(p1).add(p2).add(p3)

      //access keys
      val k1Exists: Boolean = se1.exists(k1) //true

      //access Parameters
      val p4: Option[Parameter[Int]] = se1.get(k1)

      //access values
      val v1: Array[Int] = se1(k1).values
      val v2: Array[Int] = se2.parameter(k2).values
      //k4 is missing
      val missingKeys: Set[String] = se3.missingKeys(k1, k2, k3, k4)

      //remove keys
      val se4: SystemEvent = se3.remove(k3)

      //add more than one parameters, using madd
      val se5: SystemEvent = se4.madd(k3.set("X", "Y", "Z").withUnits(Units.day), k4.set(99, 100))
      val paramSize: Int   = se5.size

      //update existing key with set
      val se6: SystemEvent = se5.add(k2.set(5, 6, 7, 8))

      //#systemevent

      //validations
      assert(k1Exists === true)
      assert(p4.get === p1)
      assert(v1 === p1.values)
      assert(v2 === p2.values)
      assert(missingKeys === Set(k4.keyName))
      assert(se2 != se4)
      assert(paramSize === 4)
      assert(se6(k2).values === Array(5, 6, 7, 8))
    }
  }

  describe("Examples of serialization") {
    it("should show reading and writing of events") {

      //#json-serialization
      import play.api.libs.json.{JsValue, Json}

      //key
      val k1: Key[MatrixData[Double]] = DoubleMatrixKey.make("myMatrix")

      val name1  = EventName("correctionInfo")
      val prefix = Prefix("aoesw.rpg")

      //values
      val m1: MatrixData[Double] = MatrixData.fromArrays(
        Array(1.0, 2.0, 3.0),
        Array(4.1, 5.1, 6.1),
        Array(7.2, 8.2, 9.2)
      )
      //parameter
      val i1: Parameter[MatrixData[Double]] = k1.set(m1)
      //events
      val observeEvent: ObserveEvent = IRDetectorEvent.observeStart(prefix.toString, ObsId("1232A-123-123")).madd(i1)
      val systemEvent: SystemEvent   = SystemEvent(prefix, name1).add(i1)

      //json support - write
      val observeJson: JsValue = JsonSupport.writeEvent(observeEvent)
      val systemJson: JsValue  = JsonSupport.writeEvent(systemEvent)

      //optionally prettify
      val str: String = Json.prettyPrint(systemJson)

      //construct command from string
      val systemEventFromPrettyStr: SystemEvent = JsonSupport.readEvent[SystemEvent](Json.parse(str))

      //json support - read
      val observeEvent1: ObserveEvent = JsonSupport.readEvent[ObserveEvent](observeJson)
      val systemEvent1: SystemEvent   = JsonSupport.readEvent[SystemEvent](systemJson)
      //#json-serialization

      //validations
      assert(observeEvent === observeEvent1)
      assert(systemEvent === systemEvent1)
      assert(systemEventFromPrettyStr === systemEvent)
    }
  }

  describe("Examples of unique key constraint") {
    it("should show duplicate keys are removed") {

      //#unique-key
      //keys
      val encoderKey: Key[Int] = KeyType.IntKey.make("encoder")
      val filterKey: Key[Int]  = KeyType.IntKey.make("filter")
      val miscKey: Key[Int]    = KeyType.IntKey.make("misc")

      //prefix
      val prefix = Prefix("wfos.blue.filter")

      val name1 = EventName("filterWheel")

      //params
      val encParam1 = encoderKey.set(1)
      val encParam2 = encoderKey.set(2)

      val encParam3    = encoderKey.set(3)
      val filterParam1 = filterKey.set(1)
      val filterParam2 = filterKey.set(2)

      val filterParam3 = filterKey.set(3)

      val miscParam1 = miscKey.set(100)
      //StatusEvent with duplicate key via constructor
      val systemEvent =
        SystemEvent(prefix, name1, Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      //four duplicate keys are removed; now contains one Encoder and one Filter key
      val uniqueKeys1 = systemEvent.paramSet.toList.map(_.keyName)

      //try adding duplicate keys via add + madd
      val changedStatusEvent = systemEvent
        .add(encParam3)
        .madd(
          filterParam1,
          filterParam2,
          filterParam3
        )
      //duplicate keys will not be added. Should contain one Encoder and one Filter key
      val uniqueKeys2 = changedStatusEvent.paramSet.toList.map(_.keyName)

      //miscKey(unique) will be added; encoderKey(duplicate) will not be added
      val finalStatusEvent = systemEvent.madd(Set(miscParam1, encParam1))
      //now contains encoderKey, filterKey, miscKey
      val uniqueKeys3 = finalStatusEvent.paramSet.toList.map(_.keyName)
      //#unique-key

      //validations
      uniqueKeys1 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)
      uniqueKeys2 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)
      uniqueKeys3 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName, miscKey.keyName)
    }
  }

  describe("Examples of Cbor") {
    it("should show usage of converting events to/from cbor | CSW-147") {
      //#cbor
      //Key
      val solarSystemKey = KeyType.SolarSystemCoordKey.make("planets")

      //values
      val planet1 = SolarSystemCoord(Tag("planet"), Jupiter)
      val planet2 = SolarSystemCoord(Tag("planet"), Venus)

      //parameters
      val param = solarSystemKey.set(planet1, planet2)

      val name = EventName("targetCoords")
      //events
      //#observe-event
      val obsId                               = ObsId("1234A-001-0123")
      val prefix                              = Prefix("tcs.pk")
      val wfsObserveEvent: ObserveEvent       = WFSDetectorEvent.publishSuccess(prefix.toString)
      val irObserveEvent: ObserveEvent        = IRDetectorEvent.observeStart(prefix.toString, obsId)
      val opdObserveEvent: ObserveEvent       = OpticalDetectorEvent.observeStart(prefix.toString, obsId)
      val sequencerObserveEvent: ObserveEvent = SequencerObserveEvent(prefix).observeStart(obsId)
      //#observe-event
      val systemEvent1: SystemEvent = SystemEvent(prefix, name).add(param)
      val systemEvent2: SystemEvent = SystemEvent(prefix, name).add(param)

      //convert events to cbor bytestring
      val byteArray2 = EventCbor.encode(wfsObserveEvent)
      val byteArray3 = EventCbor.encode(systemEvent1)
      val byteArray4 = EventCbor.encode(systemEvent2)

      //convert cbor bytestring to events
      val pbObserveEvent: ObserveEvent = EventCbor.decode(byteArray2)
      val pbSystemEvent1: SystemEvent  = EventCbor.decode(byteArray3)
      val pbSystemEvent2: SystemEvent  = EventCbor.decode(byteArray4)
      //#cbor

      //validations
      assert(pbObserveEvent === wfsObserveEvent)
      assert(pbSystemEvent1 === systemEvent1)
      assert(pbSystemEvent2 === systemEvent2)
    }
  }
}
