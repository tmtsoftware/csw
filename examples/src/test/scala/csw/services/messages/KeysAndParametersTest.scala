package csw.services.messages

import java.time.Instant

import csw.params.core.generics.KeyType.{ChoiceKey, StructKey}
import csw.params.core.generics.{GChoiceKey, Key, KeyType, Parameter}
import csw.params.core.models._
import org.scalatest.{FunSpec, Matchers}

class KeysAndParametersTest extends FunSpec with Matchers {
  describe("Examples of keys and parameters") {

    it("should show usage of primitive types") {

      //#primitives
      //declare keyname
      val s1: String = "encoder"

      //making 3 keys
      val k1: Key[Boolean] = KeyType.BooleanKey.make(s1)
      val k2: Key[Short]   = KeyType.ShortKey.make("RandomKeyName")
      val k3: Key[String]  = KeyType.StringKey.make(s1)

      //storing a single value
      val booleanParam: Parameter[Boolean] = k1.set(true)

      //storing multiple values
      val paramWithShorts1: Parameter[Short] = k2.set(1, 2, 3, 4)
      val paramWithShorts2: Parameter[Short] = k2 -> (1, 2, 3, 4)
      val paramWithShorts3: Parameter[Short] = k2 -> Array[Short](1, 2, 3, 4)

      //associating units
      val weekDays: Array[String]            = Array("Sunday", "Monday", "Tuesday")
      val paramWithUnits1: Parameter[String] = k3.set(weekDays, Units.day)
      val paramWithUnits2: Parameter[String] = k3 -> weekDays withUnits Units.day

      //deault unit is NoUnits
      assert(booleanParam.units === Units.NoUnits)

      //set units explicitly on an existing Parameter
      val paramWithUnits3: Parameter[Short] = paramWithShorts1.withUnits(Units.meter)

      //retrieve values from Parameter
      val allValues: Array[Short] = paramWithShorts1.values

      //retrieve just top value
      val head: Short = paramWithShorts1.head
      //#primitives

      //validations
      assert(head === 1)
      assert(allValues === Array(1, 2, 3, 4))
      assert(paramWithShorts1.values === paramWithShorts2.values)
      assert(paramWithShorts2.values === paramWithShorts3.values)
      assert(paramWithUnits3.units === Units.meter)
      assert(paramWithUnits1.values === paramWithUnits2.values)
    }

    it("should show usage of arrays") {
      //#arrays
      //make some arrays
      val arr1: Array[Double] = Array(1.0, 2.0, 3.0, 4.0, 5.0)
      val arr2: Array[Double] = Array(10.0, 20.0, 30.0, 40.0, 50.0)

      //keys
      val filterKey: Key[ArrayData[Double]] = KeyType.DoubleArrayKey.make("filter")

      //Store some values using helper class ArrayData
      val p1: Parameter[ArrayData[Double]] = filterKey.set(ArrayData(arr1), ArrayData(arr2))
      val p2: Parameter[ArrayData[Double]] = filterKey -> ArrayData(arr1 ++ arr2) withUnits Units.liter

      //add units to existing parameters
      val p1AsCount = p1.withUnits(Units.count)

      //default unit is NoUnits
      assert(p1.units === Units.NoUnits)

      //retrieving values
      val head: Array[Double]                 = p1.head.data.toArray
      val allValues: Array[ArrayData[Double]] = p1.values
      //#arrays

      //validations
      assert(head === arr1)
      assert(allValues === Array(ArrayData(arr1), ArrayData(arr2)))
      assert(p2.values === Array(ArrayData.fromArray(arr1 ++ arr2)))
      assert(p1AsCount.units === Units.count)
    }

    it("should show usage of matrices") {
      //#matrices
      //make some arrays
      val m1: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9))
      val m2: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3, 4, 5), Array[Byte](10, 20, 30, 40, 50))

      //keys
      val encoderKey: Key[MatrixData[Byte]] = KeyType.ByteMatrixKey.make("encoder")

      //Store some values using helper class MatrixData
      val p1: Parameter[MatrixData[Byte]] = encoderKey.set(MatrixData.fromArrays(m1))
      val p2: Parameter[MatrixData[Byte]] = encoderKey.set(m1, m2) withUnits Units.liter

      //add units to existing parameters
      val p1AsLiter = p1.withUnits(Units.liter)

      //default unit is NoUnits
      assert(p1.units === Units.NoUnits)

      //retrieving values
      val head: Array[Array[Byte]]           = p1.head.data.map(_.toArray).toArray
      val allValues: Array[MatrixData[Byte]] = p1.values
      //#matrices

      //validations
      assert(head === m1)
      assert(allValues === Array(MatrixData.fromArrays(m1)))
      assert(p2.values === Array(MatrixData.fromArrays(m1), MatrixData.fromArrays(m2)))
      assert(p1AsLiter.units === Units.liter)
    }

    it("should show usage of choice") {
      //#choice
      //Choice
      val choices = Choices.from("A", "B", "C")

      //keys
      val choice1Key: GChoiceKey = ChoiceKey.make("mode", choices)
      val choice2Key: GChoiceKey = ChoiceKey.make(
        "mode-reset",
        Choices.fromChoices(Choice("c"), Choice("b"), Choice("a"))
      )

      //store values
      val p1: Parameter[Choice] = choice1Key
        .set(Array(Choice("A")))
        .withUnits(Units.foot)
      val p2: Parameter[Choice] = choice2Key.set(Array(Choice("c")))

      //add units
      val paramWithFoot = p1.withUnits(Units.foot)

      //default unit is NoUnits
      assert(p2.units === Units.NoUnits)

      //retrieving values
      val head: Choice          = p1.head
      val values: Array[Choice] = p2.values

      //#choice
      //validations
      head should be(Choice("A"))
      values should be(Array(Choice("c")))
      paramWithFoot.units should be(Units.foot)
    }

    it("should show usage of radec") {
      //#radec
      //RaDec
      val raDec1: RaDec = RaDec(1.0, 2.0)
      val raDec2: RaDec = RaDec(3.0, 4.0)

      //keys
      val raDecKey: Key[RaDec] = KeyType.RaDecKey.make("raDecKey")

      //store values
      val p1: Parameter[RaDec] = raDecKey.set(raDec1)
      val p2: Parameter[RaDec] = raDecKey -> Array(raDec1, raDec2)

      //add units
      val paramWithDegree = p1.withUnits(Units.degree)

      //default unit is NoUnits
      assert(p2.units === Units.NoUnits)

      //retrieving values
      val head: RaDec          = p1.head
      val values: Array[RaDec] = p2.values

      //#radec
      //validations
      head should be(raDec1)
      values should be(Array(raDec1, raDec2))
      paramWithDegree.units should be(Units.degree)
    }

    it("should show usage of struct") {
      //#struct
      //keys
      val skey: Key[Struct] = StructKey.make("myStruct")

      val ra    = KeyType.StringKey.make("ra")
      val dec   = KeyType.StringKey.make("dec")
      val epoch = KeyType.DoubleKey.make("epoch")

      //initialize struct
      val struct1: Struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))
      val struct2: Struct = Struct().madd(dec.set("32:33:34.4"), ra.set("12:13:14.1"), epoch.set(1970.0))

      //make parameters
      val p1: Parameter[Struct] = skey.set(struct1)
      val p2: Parameter[Struct] = skey.set(struct1, struct2)

      //add units
      val paramWithLightYear = p1.withUnits(Units.lightyear)

      //default unit is NoUnits
      assert(p2.units === Units.NoUnits)

      //retrieving values
      val head: Struct          = p1.head
      val values: Array[Struct] = p2.values

      //get individual keys
      val firstKey: Option[Parameter[String]]  = struct1.get(KeyType.StringKey.make("ra"))
      val secondKey: Option[Parameter[String]] = struct1.get("dec", KeyType.StringKey)
      val thirdKey: Option[Parameter[Double]]  = struct1.get("epoch", KeyType.DoubleKey)

      //access parameter using 'parameter' or 'apply' method
      assert(struct1.parameter(ra) === struct1(ra))

      //remove a parameter and verify it doesn't exist
      val mutated1: Struct = struct1.remove(ra) //using key
      val mutated2         = struct1.remove(firstKey.get)
      assert(mutated1.exists(ra) === false)
      assert(mutated2.exists(ra) === false)

      //find out missing keys
      val missingKeySet: Set[String] = mutated1.missingKeys(ra, dec, epoch, KeyType.FloatKey.make("missingKey"))
      assert(missingKeySet === Set("ra", "missingKey"))

      //#struct
      //validations
      head should be(struct1)
      values should be(Array(struct1, struct2))
      paramWithLightYear.units should be(Units.lightyear)

      assert(mutated1 === mutated2)

      firstKey.get shouldBe struct1.parameter(ra)
      secondKey.get shouldBe struct1.parameter(dec)
      thirdKey.get shouldBe struct1.parameter(epoch)
    }

    it("should show usage of units") {

      //#units
      //declare keyname
      val s1: String = "encoder"

      //making 3 keys
      val k1: Key[Boolean] = KeyType.BooleanKey.make(s1)
      val k2: Key[Short]   = KeyType.ShortKey.make("RandomKeyName")
      val k3: Key[String]  = KeyType.StringKey.make(s1)

      //storing a single value, default unit is NoUnits
      val bParam: Parameter[Boolean] = k1.set(true)
      val bDefaultUnitSet: Boolean   = bParam.units === Units.NoUnits //true

      //default unit for TimestampKey
      val tParam: Parameter[Instant] = KeyType.TimestampKey.make("now").set(Instant.now())
      val defaultTimeUnit: Units     = tParam.units //is second

      //storing multiple values
      val paramOfShorts: Parameter[Short] = k2.set(1, 2, 3, 4)

      //values to store
      val weekDays: Array[String] = Array("Sunday", "Monday", "Tuesday")

      //associating units via set
      val paramWithUnits1: Parameter[String] = k3.set(weekDays, Units.day)
      //associating units via withUnits
      val paramWithUnits2: Parameter[String] = k3 -> weekDays withUnits Units.count
      //change existing unit
      val paramWithUnits3: Parameter[Short] = paramOfShorts.withUnits(Units.meter)
      //#units

      //validations
      assert(bDefaultUnitSet === true)
      assert(defaultTimeUnit === Units.second)
      assert(paramWithUnits1.units === Units.day)
      assert(paramWithUnits2.units === Units.count)
      assert(paramWithUnits3.units === Units.meter)
    }
  }
}
