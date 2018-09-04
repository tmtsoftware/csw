package csw.services.messages

import java.time.Instant

import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.{DoubleMatrixKey, RaDecKey}
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.params.models.Units.arcmin
import csw.messages.params.models._
import csw.messages.params.pb.PbConverter
import csw_protobuf.events.PbEvent
import org.scalatest.{FunSpec, Matchers}

class EventsTest extends FunSpec with Matchers {

  describe("Examples of EventTime") {
    it("should show usage of utility functions") {
      //#eventtime
      //default constructor will return current time in UTC
      val now: EventTime = EventTime()

      //using constructor
      val anHourAgo: EventTime = EventTime(Instant.now().minusSeconds(3600))

      //current event time using utility function
      val currentTime: EventTime = EventTime()

      //some past time using utility function
      val aDayAgo = EventTime(Instant.now.minusSeconds(86400))

      //#eventtime

      //validations
      assert(now.time.isAfter(anHourAgo.time))
      assert(anHourAgo.time.isAfter(aDayAgo.time))
      assert(currentTime.time.isAfter(anHourAgo.time))
    }
  }

  describe("Examples of Events") {
    it("should show usages of ObserveEvent") {

      //#observeevent
      //keys
      val k1: Key[Int]    = KeyType.IntKey.make("readoutsCompleted")
      val k2: Key[Int]    = KeyType.IntKey.make("coaddsCompleted")
      val k3: Key[String] = KeyType.StringKey.make("fileID")
      val k4: Key[Int]    = KeyType.IntKey.make("notUsed")

      //prefixes
      val ck1   = Prefix("iris.ifu.detectorAssembly")
      val name1 = EventName("readoutEnd")
      val ck3   = Prefix("wfos.red.detector")
      val name3 = EventName("exposureStarted")

      //parameters
      val p1: Parameter[Int]    = k1.set(4)
      val p2: Parameter[Int]    = k2.set(2)
      val p3: Parameter[String] = k3.set("WFOS-RED-0001")

      //Create ObserveEvent using madd
      val se1: ObserveEvent = ObserveEvent(ck1, name1).madd(p1, p2)
      //Create ObserveEvent using apply
      val se2: ObserveEvent = ObserveEvent(ck3, name3, Set(p1, p2))
      //Create ObserveEvent and use add
      val se3: ObserveEvent = ObserveEvent(ck3, name3).add(p1).add(p2).add(p3)

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
      val se4: ObserveEvent = se3.remove(k3)

      //#observeevent

      //validations
      assert(k1Exists === true)
      assert(p4.get === p1)
      assert(v1 === p1.values)
      assert(v2 === p2.values)
      assert(missingKeys === Set(k4.keyName))
      assert(se2 != se4)
    }

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
      val observeEvent: ObserveEvent = ObserveEvent(prefix, name1).add(i1)
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

  describe("Examples of protobuf") {
    it("should show usage of converting events to/from protobuf") {
      //#protobuf
      //Key
      val raDecKey = RaDecKey.make("raDecKey")

      //values
      val raDec1 = RaDec(10.20, 40.20)
      val raDec2 = RaDec(11.20, 50.20)

      //parameters
      val param = raDecKey.set(raDec1, raDec2).withUnits(arcmin)

      val prefix = Prefix("tcs.pk")
      val name   = EventName("targetCoords")
      //events
      val observeEvent: ObserveEvent = ObserveEvent(prefix, name).add(param)
      val systemEvent1: SystemEvent  = SystemEvent(prefix, name).add(param)
      val systemEvent2: SystemEvent =
        SystemEvent(prefix, name).add(param)

      //convert events to protobuf bytestring
      val byteArray2: PbEvent = PbConverter.toPbEvent(observeEvent)
      val byteArray3: PbEvent = PbConverter.toPbEvent(systemEvent1)
      val byteArray4: PbEvent = PbConverter.toPbEvent(systemEvent2)

      //convert protobuf bytestring to events
      val pbObserveEvent: ObserveEvent = PbConverter.fromPbEvent(byteArray2)
      val pbSystemEvent1: SystemEvent  = PbConverter.fromPbEvent(byteArray3)
      val pbSystemEvent2: SystemEvent  = PbConverter.fromPbEvent(byteArray4)
      //#protobuf

      //validations
      assert(pbObserveEvent === observeEvent)
      assert(pbSystemEvent1 === systemEvent1)
      assert(pbSystemEvent2 === systemEvent2)
    }
  }
}
