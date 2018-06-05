package csw.services.messages

import java.time.Instant

import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.DoubleMatrixKey
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.params.models.Units.{meter, NoUnits}
import csw.messages.params.models.{MatrixData, Prefix}
import csw.messages.params.states.{CurrentState, DemandState, StateName}
import org.scalatest.{FunSpec, Matchers}

class StateVariablesTest extends FunSpec with Matchers {
  describe("Examples of State variables") {

    it("should show usages of DemandState") {

      //#demandstate
      //prefix
      val prefix = Prefix("wfos.prog.cloudcover")

      //key
      val charKey: Key[Char]         = KeyType.CharKey.make("charKey")
      val intKey: Key[Int]           = KeyType.IntKey.make("intKey")
      val booleanKey: Key[Boolean]   = KeyType.BooleanKey.make("booleanKey")
      val timestampKey: Key[Instant] = KeyType.TimestampKey.make("timestampKey")
      val notUsedKey: Key[String]    = KeyType.StringKey.make("notUsed")

      //parameters
      val charParam: Parameter[Char]       = charKey.set('A', 'B', 'C').withUnits(NoUnits)
      val intParam: Parameter[Int]         = intKey.set(1, 2, 3).withUnits(meter)
      val booleanParam: Parameter[Boolean] = booleanKey.set(true, false)
      val timestamp: Parameter[Instant]    = timestampKey.set(Instant.now)

      //create DemandState and use sequential add
      val ds1: DemandState = DemandState(prefix, StateName("testStateName")).add(charParam).add(intParam)
      //create DemandState and add more than one Parameters using madd
      val ds2: DemandState = DemandState(prefix, StateName("testStateName")).madd(intParam, booleanParam)
      //create DemandState using apply
      val ds3: DemandState = DemandState(prefix, StateName("testStateName"), Set(timestamp))

      //access keys
      val charKeyExists: Boolean = ds1.exists(charKey) //true

      //access Parameters
      val p1: Option[Parameter[Int]] = ds1.get(intKey)

      //access values
      val v1: Array[Char]    = ds1(charKey).values
      val v2: Array[Boolean] = ds2.parameter(booleanKey).values
      val missingKeys: Set[String] = ds3.missingKeys(
        charKey,
        intKey,
        booleanKey,
        timestampKey,
        notUsedKey
      )

      //remove keys
      val ds4: DemandState = ds3.remove(timestampKey)

      //update existing keys - set it back by an hour
      val ds5: DemandState = ds3.add(timestampKey.set(Instant.now().minusSeconds(3600)))

      //#demandstate
      //validations
      assert(charKeyExists === true)
      assert(p1.get === intParam)
      assert(v1 === Array('A', 'B', 'C'))
      assert(v2 === Array(true, false))
      assert(missingKeys.size === 4)
      assert(ds4.exists(timestampKey) === false)
      assert(ds5(timestampKey).head.isBefore(ds3(timestampKey).head))
    }

    it("should show usages of CurrentState") {

      //#currentstate

      //prefix
      val prefix = Prefix("wfos.prog.cloudcover")

      //key
      val charKey      = KeyType.CharKey.make("charKey")
      val intKey       = KeyType.IntKey.make("intKey")
      val booleanKey   = KeyType.BooleanKey.make("booleanKey")
      val timestampKey = KeyType.TimestampKey.make("timestampKey")
      val notUsedKey   = KeyType.StringKey.make("notUsed")

      //parameters
      val charParam    = charKey.set('A', 'B', 'C').withUnits(NoUnits)
      val intParam     = intKey.set(1, 2, 3).withUnits(meter)
      val booleanParam = booleanKey.set(true, false)
      val timestamp    = timestampKey.set(Instant.now)

      //create CurrentState and use sequential add
      val cs1 = CurrentState(prefix, StateName("testStateName")).add(charParam).add(intParam)
      //create CurrentState and add more than one Parameters using madd
      val cs2 = CurrentState(prefix, StateName("testStateName")).madd(intParam, booleanParam)
      //create CurrentState using apply
      val cs3 = CurrentState(prefix, StateName("testStateName"), Set(timestamp))

      //access keys
      val charKeyExists = cs1.exists(charKey) //true

      //access Parameters
      val p1: Option[Parameter[Int]] = cs1.get(intKey)

      //access values
      val v1: Array[Char]    = cs1(charKey).values
      val v2: Array[Boolean] = cs2.parameter(booleanKey).values
      val missingKeys: Set[String] = cs3.missingKeys(
        charKey,
        intKey,
        booleanKey,
        timestampKey,
        notUsedKey
      )

      //remove keys
      val cs4 = cs3.remove(timestampKey)

      //update existing keys - set it back by an hour
      val cs5 = cs3.add(timestampKey.set(Instant.now().minusSeconds(3600)))

      //#currentstate

      //validations
      assert(charKeyExists === true)
      assert(p1.get === intParam)
      assert(v1 === Array('A', 'B', 'C'))
      assert(v2 === Array(true, false))
      assert(missingKeys.size === 4)
      assert(cs4.exists(timestampKey) === false)
      assert(cs5(timestampKey).head.isBefore(cs3(timestampKey).head))
    }
  }

  describe("Examples of serialization") {
    it("should show reading and writing of State variables") {
      //#json-serialization
      import play.api.libs.json.{JsValue, Json}

      //key
      val k1: Key[MatrixData[Double]] = DoubleMatrixKey.make("myMatrix")
      //values
      val m1: MatrixData[Double] = MatrixData.fromArrays(
        Array(1.0, 2.0, 3.0),
        Array(4.1, 5.1, 6.1),
        Array(7.2, 8.2, 9.2)
      )

      //parameter
      val p1: Parameter[MatrixData[Double]] = k1.set(m1)

      //state variables
      val ds: DemandState  = DemandState(Prefix("wfos.blue.filter"), StateName("testStateName")).add(p1)
      val cs: CurrentState = CurrentState(Prefix("wfos.blue.filter"), StateName("testStateName")).add(p1)

      //json support - write
      val dsJson: JsValue = JsonSupport.writeStateVariable(ds)
      val csJson: JsValue = JsonSupport.writeStateVariable(cs)

      //optionally prettify
      val str: String = Json.prettyPrint(dsJson)

      //construct command from string
      val scFromPrettyStr = JsonSupport.readStateVariable[DemandState](Json.parse(str))

      //json support - read
      val ds1: DemandState  = JsonSupport.readStateVariable[DemandState](dsJson)
      val cs1: CurrentState = JsonSupport.readStateVariable[CurrentState](csJson)
      //#json-serialization

      //validations
      assert(ds === ds1)
      assert(cs === cs1)
      assert(scFromPrettyStr === ds)
    }
  }

  describe("Examples of unique key constraint") {
    it("should show duplicate keys are removed") {

      //#unique-key
      //keys
      val encoderKey: Key[Int] = KeyType.IntKey.make("encoder")
      val filterKey: Key[Int]  = KeyType.IntKey.make("filter")
      val miscKey: Key[Int]    = KeyType.IntKey.make("misc.")

      //prefix
      val prefix = Prefix("wfos.blue.filter")

      //params
      val encParam1 = encoderKey.set(1)
      val encParam2 = encoderKey.set(2)
      val encParam3 = encoderKey.set(3)

      val filterParam1 = filterKey.set(1)
      val filterParam2 = filterKey.set(2)
      val filterParam3 = filterKey.set(3)

      val miscParam1 = miscKey.set(100)

      //DemandState with duplicate key via constructor
      val statusEvent = DemandState(
        prefix,
        StateName("testStateName"),
        Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3)
      )
      //four duplicate keys are removed; now contains one Encoder and one Filter key
      val uniqueKeys1 = statusEvent.paramSet.toList.map(_.keyName)

      //try adding duplicate keys via add + madd
      val changedStatusEvent = statusEvent
        .add(encParam3)
        .madd(
          filterParam1,
          filterParam2,
          filterParam3
        )
      //duplicate keys will not be added. Should contain one Encoder and one Filter key
      val uniqueKeys2 = changedStatusEvent.paramSet.toList.map(_.keyName)

      //miscKey(unique) will be added; encoderKey(duplicate) will not be added
      val finalStatusEvent = statusEvent.madd(Set(miscParam1, encParam1))
      //now contains encoderKey, filterKey, miscKey
      val uniqueKeys3 = finalStatusEvent.paramSet.toList.map(_.keyName)
      //#unique-key

      //validations
      uniqueKeys1 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)
      uniqueKeys2 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)
      uniqueKeys3 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName, miscKey.keyName)
    }
  }
}
