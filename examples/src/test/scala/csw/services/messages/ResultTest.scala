package csw.services.messages

import csw.messages.commands.Result
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.DoubleMatrixKey
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.params.models._
import org.scalatest.{FunSpec, Matchers}

class ResultTest extends FunSpec with Matchers {

  //#runid
  val runId: Id = Id()
  //#runid

  describe("Examples of Result") {

    it("Should show usage of Result") {
      //#result
      //keys
      val k1: Key[Int]    = KeyType.IntKey.make("encoder")
      val k2: Key[Int]    = KeyType.IntKey.make("windspeed")
      val k3: Key[String] = KeyType.StringKey.make("filter")
      val k4: Key[Int]    = KeyType.IntKey.make("notUsed")

      //prefixes
      val prefix = Prefix("wfos.prog.cloudcover")

      //parameters
      val p1: Parameter[Int]    = k1.set(22)
      val p2: Parameter[Int]    = k2.set(44)
      val p3: Parameter[String] = k3.set("A", "B", "C", "D")

      //Create Result using madd
      val r1: Result = Result(prefix).madd(p1, p2)
      //Create Result using apply
      val r2: Result = Result(prefix, Set(p1, p2))
      //Create Result and use add
      val r3: Result = Result(prefix).add(p1).add(p2).add(p3)

      //access keys
      val k1Exists: Boolean = r1.exists(k1) //true

      //access Parameters
      val p4: Option[Parameter[Int]] = r1.get(k1)

      //access values
      val v1: Array[Int] = r1(k1).values
      val v2: Array[Int] = r2.parameter(k2).values
      //k4 is missing
      val missingKeys: Set[String] = r3.missingKeys(k1, k2, k3, k4)

      //remove keys
      val r4: Result = r3.remove(k3)
      //#result

      //validations
      assert(k1Exists === true)
      assert(p4.get === p1)
      assert(v1 === p1.values)
      assert(v2 === p2.values)
      assert(missingKeys === Set(k4.keyName))
      assert(r2 === r4)
    }
  }

  describe("Examples of serialization") {
    it("should show reading and writing of commands") {

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

      //prefixes
      val prefix = Prefix("wfos.prog.cloudcover")

      //parameter
      val i1: Parameter[MatrixData[Double]] = k1.set(m1)

      //result
      val result: Result = Result(prefix).add(i1)

      //json support - write
      val resultJson: JsValue = JsonSupport.writeResult(result)

      //optionally prettify
      val str: String = Json.prettyPrint(resultJson)

      //construct result from string
      val scFromPrettyStr: Result = JsonSupport.readResult(Json.parse(str))

      //json support - read
      val result1: Result = JsonSupport.readResult(resultJson)
      //#json-serialization

      //validations
      assert(result === result1)
      assert(scFromPrettyStr === result)
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

      //Setup command with duplicate key via constructor
      val result = Result(prefix, Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      //four duplicate keys are removed; now contains one Encoder and one Filter key
      val uniqueKeys1 = result.paramSet.toList.map(_.keyName)

      //try adding duplicate keys via add + madd
      val changedResult = result
        .add(encParam3)
        .madd(
          filterParam1,
          filterParam2,
          filterParam3
        )
      //duplicate keys will not be added. Should contain one Encoder and one Filter key
      val uniqueKeys2 = changedResult.paramSet.toList.map(_.keyName)

      //miscKey(unique) will be added; encoderKey(duplicate) will not be added
      val finalResult = result.madd(Set(miscParam1, encParam1))
      //now contains encoderKey, filterKey, miscKey
      val uniqueKeys3 = finalResult.paramSet.toList.map(_.keyName)
      //#unique-key

      //validations
      uniqueKeys1 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)
      uniqueKeys2 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)
      uniqueKeys3 should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName, miscKey.keyName)
    }
  }
}
