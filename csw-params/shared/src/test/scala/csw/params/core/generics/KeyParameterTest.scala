package csw.params.core.generics

import csw.params.core.generics.KeyType._
import csw.params.core.models.Coords.EqFrame.FK5
import csw.params.core.models.Coords.SolarSystemObject.Venus
import csw.params.core.models.Units._
import csw.params.core.models._
import csw.time.core.models.{TAITime, UTCTime}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
// DEOPSCSW-188: Efficient Serialization to/from JSON
// DEOPSCSW-184: Change configurations - attributes and values
// DEOPSCSW-196: Command Payloads for variable command content
// DEOPS-CSW-MAINT-116: Restrict "[" , "]" and "/" chars in key name of Parameter model
class KeyParameterTest extends AnyFunSpec with Matchers {

  private val s1: String = "encoder"
  private val s2: String = "filter"

  describe("basic key tests") {
    val k1: Key[Int] = KeyType.IntKey.make(s1, Units.count)
    val k2: Key[Int] = KeyType.IntKey.make(s2, Units.NoUnits)
    it("should have correct name | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      assert(k1.keyName.equals(s1))
    }

    val k3: Key[Int] = KeyType.IntKey.make(s1, Units.encoder)

    it("should have equality based on name | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      assert(k3 == k1)
      assert(k3 != k2)
      assert(k1 != k2)
    }
  }

  describe("test booleanKey") {

    val tval = false
    val bk   = KeyType.BooleanKey.make(s1)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val ii = bk.set(tval)
      ii.values should be(Array(tval))
      ii.get(0).get should equal(tval)
    }

    val listIn = Array(false, true)

    // DEOPSCSW-190: Implement Unit Support
    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-190") {
      val ii = bk.setAll(listIn).withUnits(degree)
      ii.units should be(degree)
      ii.value(1) should equal(listIn(1))
      ii.values should equal(listIn)
    }

    // DEOPSCSW-190: Implement Unit Support
    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-190") {
      val ii = bk.setAll(listIn)
      ii.units should be(NoUnits)
      ii.value(1) should equal(listIn(1))
      ii.values should equal(listIn)
    }
  }

  describe("test charItem") {
    val tval = 'K'
    val lk   = KeyType.CharKey.make(s1, encoder)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.set(tval)
      li.values should be(Array(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Array[Char]('K', 'G')

    // DEOPSCSW-190: Implement Unit Support
    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-190") {
      val li = lk.setAll(listIn).withUnits(degree)
      li.units should be(degree)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn)
      li.units should be(encoder)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  // DEOPSCSW-186: Binary value payload
  describe("test ByteItem") {
    val tval: Byte = 123
    val lk         = KeyType.ByteKey.make(s1, encoder)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186") {
      val li = lk.set(tval)
      li.values should be(Array(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val byteIn = Array[Byte](121, 122)

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186") {
      val li = lk.setAll(byteIn).withUnits(degree)
      li.units should be(degree)
      li.value(0) should equal(byteIn(0))
      li.value(1) should equal(byteIn(1))
      li.values should equal(byteIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186") {
      val li = lk.setAll(byteIn)
      li.units should be(encoder)
      li.value(1) should equal(byteIn(1))
      li.values should equal(byteIn)
    }
  }

  // DEOPSCSW-186: Binary value payload
  describe("test ByteArrayKey") {
    val a1: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)
    val a2: Array[Byte] = Array[Byte](10, 20, 30, 40, 50)

    val la1 = ArrayData(a1)
    val la2 = ArrayData(a2)
    val lk  = KeyType.ByteArrayKey.make(s1, encoder)

    it("should test single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186") {
      val di: Parameter[ArrayData[Byte]] = lk.set(la1)
      di.values should equal(Array(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Array(la1, la2)

    // DEOPSCSW-190: Implement Unit Support
    it(
      "should test with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186, DEOPSCSW-190"
    ) {
      val li2: Parameter[ArrayData[Byte]] = lk.setAll(listIn).withUnits(degree)
      li2.units should be(degree)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    // DEOPSCSW-190: Implement Unit Support
    it(
      "should test with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186, DEOPSCSW-190"
    ) {
      val li2 = lk.setAll(listIn)
      li2.units should be theSameInstanceAs encoder
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    // DEOPSCSW-190: Implement Unit Support
    it(
      "should test using one array with and without units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186, DEOPSCSW-190"
    ) {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degree)
      li2.units should be theSameInstanceAs degree
      li2.head should equal(la2)
    }

    it("should test using var args | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186") {
      val li3: Parameter[ArrayData[Byte]] = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Byte] = Array[Byte](1, 2, 3)
      val b: Array[Byte] = Array[Byte](10, 20, 30)
      val c: Array[Byte] = Array[Byte](100, 101, 102)

      val li4: Parameter[ArrayData[Byte]] = lk.set(a, b, c).withUnits(meter)
      li4.values.length should be(3)
      li4.value(2) should equal(ArrayData(c))
    }

  }

  // DEOPSCSW-186: Binary value payload
  describe("test byteMatrixKey") {
    val m1: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9))
    val m2: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3, 4, 5), Array[Byte](10, 20, 30, 40, 50))

    val lm1 = MatrixData.fromArrays(m1)
    val lm2 = MatrixData.fromArrays(m2)
    val dk  = ByteMatrixKey.make(s1, encoder)

    it("should work with a single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186") {
      val di = dk.set(lm1)
      di.values should equal(Array(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Array(lm1, lm2)

    // DEOPSCSW-190: Implement Unit Support
    it(
      "should work with list and withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186"
    ) {
      val di = dk.setAll(listIn).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    // DEOPSCSW-190: Implement Unit Support
    it(
      "should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186, DEOPSCSW-190"
    ) {
      val di = dk.setAll(listIn)
      di.units should be theSameInstanceAs encoder
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    // DEOPSCSW-190: Implement Unit Support
    it(
      "work with one matrix without and with units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186, DEOPSCSW-190"
    ) {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.head should equal(lm1)
    }

    // DEOPSCSW-190: Implement Unit Support
    it("work with varargs | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186, DEOPSCSW-190") {
      val di = dk.set(lm1, lm2).withUnits(second)
      di.units should be theSameInstanceAs second
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    // DEOPSCSW-190: Implement Unit Support
    it(
      "work with varargs as arrays | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-186, DEOPSCSW-190"
    ) {
      val di = dk.set(m1, m2).withUnits(meter)
      di.units should be theSameInstanceAs meter
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test ShortItem") {
    val tval: Short = 1234
    val lk          = KeyType.ShortKey.make(s1, encoder)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.set(tval)
      li.values should be(Array(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Array[Short](123, 456)

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn).withUnits(degree)
      li.units should be(degree)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn)
      li.units should be(encoder)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test shortArrayKey") {
    val a1 = Array[Short](1, 2, 3, 4, 5)
    val a2 = Array[Short](10, 20, 30, 40, 50)

    val la1 = ArrayData(a1)
    val la2 = ArrayData(a2)
    val lk  = KeyType.ShortArrayKey.make(s1, encoder)

    it("should test single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = lk.set(la1)
      di.values should equal(Array(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Array(la1, la2)

    it("should test with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn).withUnits(degree)
      li2.units should be(degree)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn)
      li2.units should be theSameInstanceAs encoder
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it(
      "should test using one array with and without units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196"
    ) {
      var li2 = lk.set(a1) // Uses implicit to create from int array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degree)
      li2.units should be theSameInstanceAs degree
      li2.head should equal(la2)
    }

    it("should test using var args | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Short] = Array[Short](1, 2, 3)
      val b: Array[Short] = Array[Short](10, 20, 30)
      val c: Array[Short] = Array[Short](100, 200, 300)

      val li4 = lk.set(a, b, c).withUnits(meter)
      li4.values.length should be(3)
      li4.value(2) should equal(ArrayData(c))
    }
  }

  describe("test shortMatrixKey") {
    val m1: Array[Array[Short]] = Array(Array[Short](1, 2, 3), Array[Short](4, 5, 6), Array[Short](7, 8, 9))
    val m2: Array[Array[Short]] = Array(Array[Short](1, 2, 3, 4, 5), Array[Short](10, 20, 30, 40, 50))

    val lm1 = MatrixData.fromArrays(m1)
    val lm2 = MatrixData.fromArrays(m2)
    val dk  = ShortMatrixKey.make(s1, encoder)

    it("should work with a single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1)
      di.values should equal(Array(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Array(lm1, lm2)

    it("should work with list and withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn)
      di.units should be theSameInstanceAs encoder
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.head should equal(lm1)
    }

    it("work with varargs | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1, lm2).withUnits(second)
      di.units should be theSameInstanceAs second
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(m1, m2).withUnits(meter)
      di.units should be theSameInstanceAs meter
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test IntItem") {
    val tval: Int = 1234
    val lk        = KeyType.IntKey.make(s1, encoder)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.set(tval)
      li.values should be(Array(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Array[Int](123, 456)

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn).withUnits(degree)
      li.units should be(degree)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn)
      li.units should be(encoder)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test intArrayKey") {
    val a1 = Array[Int](1, 2, 3, 4, 5)
    val a2 = Array[Int](10, 20, 30, 40, 50)

    val la1 = ArrayData(a1)
    val la2 = ArrayData(a2)
    val lk  = KeyType.IntArrayKey.make(s1, encoder)

    it("should test single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = lk.set(la1)
      di.values should equal(Array(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Array(la1, la2)

    it("should test with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn).withUnits(degree)
      li2.units should be(degree)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn)
      li2.units should be theSameInstanceAs encoder
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it(
      "should test using one array with and without units  | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196"
    ) {
      var li2 = lk.set(a1) // Uses implicit to create from int array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degree)
      li2.units should be theSameInstanceAs degree
      li2.head should equal(la2)
    }

    it("should test using var args | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Int] = Array(1, 2, 3)
      val b: Array[Int] = Array(10, 20, 30)
      val c: Array[Int] = Array(100, 200, 300)

      val li4 = lk.set(a, b, c).withUnits(meter)
      li4.values.length should be(3)
      li4.value(2) should equal(ArrayData(c))
    }
  }

  describe("test intMatrixKey") {
    val m1: Array[Array[Int]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
    val m2: Array[Array[Int]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

    val lm1 = MatrixData.fromArrays(m1)
    val lm2 = MatrixData.fromArrays(m2)
    val dk  = IntMatrixKey.make(s1, encoder)

    it("should work with a single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1)
      di.values should equal(Array(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Array(lm1, lm2)

    it("should work with list and withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn)
      di.units should be theSameInstanceAs encoder
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.head should equal(lm1)
    }

    it("work with varargs | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1, lm2).withUnits(second)
      di.units should be theSameInstanceAs second
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(m1, m2).withUnits(meter)
      di.units should be theSameInstanceAs meter
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test longKey") {
    val lval          = 1234L
    val lk: Key[Long] = KeyType.LongKey.make(s1, encoder)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.set(lval)
      li.values should be(Array(lval))
      li.get(0).get should equal(lval)
    }

    val listIn = Array[Long](123L, 456L)

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn).withUnits(degree)
      li.units should be(degree)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn)
      li.units should be(encoder)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test LongArrayKey") {
    val a1: Array[Long] = Array(1, 2, 3, 4, 5)
    val a2: Array[Long] = Array(10, 20, 30, 40, 50)

    val la1: ArrayData[Long]     = ArrayData(a1)
    val la2: ArrayData[Long]     = ArrayData(a2)
    val lk: Key[ArrayData[Long]] = KeyType.LongArrayKey.make(s1, encoder)

    it("should test single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di: Parameter[ArrayData[Long]] = lk.set(la1)
      di.values should equal(Array(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Array(la1, la2)

    it("should test with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2: Parameter[ArrayData[Long]] = lk.setAll(listIn).withUnits(degree)
      li2.units should be(degree)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn)
      li2.units should be theSameInstanceAs encoder
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it(
      "should test using one array with and without units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196"
    ) {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degree)
      li2.units should be theSameInstanceAs degree
      li2.head should equal(la2)
    }

    it("should test using var args | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li3: Parameter[ArrayData[Long]] = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Long] = Array(1, 2, 3)
      val b: Array[Long] = Array(10, 20, 30)
      val c: Array[Long] = Array(100, 200, 300)

      val li4: Parameter[ArrayData[Long]] = lk.set(a, b, c).withUnits(meter)
      li4.values.length should be(3)
      li4.value(2) should equal(ArrayData(c))
    }
  }

  describe("test longMatrixKey") {
    val m1: Array[Array[Long]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
    val m2: Array[Array[Long]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

    val lm1: MatrixData[Long]     = MatrixData.fromArrays(m1)
    val lm2: MatrixData[Long]     = MatrixData.fromArrays(m2)
    val dk: Key[MatrixData[Long]] = LongMatrixKey.make(s1, encoder)

    it("should work with a single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1)
      di.values should equal(Array(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Array(lm1, lm2)

    it("should work with list and withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn)
      di.units should be theSameInstanceAs encoder
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.head should equal(lm1)
    }

    it("work with varargs | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1, lm2).withUnits(second)
      di.units should be theSameInstanceAs second
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(m1, m2).withUnits(meter)
      di.units should be theSameInstanceAs meter
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test floatItem") {
    val tval: Float = 123.456f
    val lk          = KeyType.FloatKey.make(s1, encoder)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.set(tval)
      li.values should be(Array(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Array[Float](123.0f, 456.0f)

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn).withUnits(degree)
      li.units should be(degree)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn)
      li.units should be(encoder)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test floatArrayKey") {
    val a1 = Array[Float](1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
    val a2 = Array[Float](10.0f, 20.0f, 30.0f, 40.0f, 50.0f)

    val la1 = ArrayData(a1)
    val la2 = ArrayData(a2)
    val lk  = KeyType.FloatArrayKey.make(s1, encoder)

    it("should test single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = lk.set(la1)
      di.values should equal(Array(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Array(la1, la2)

    it("should test with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn).withUnits(degree)
      li2.units should be(degree)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn)
      li2.units should be theSameInstanceAs encoder
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it(
      "should test using one array with and without units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196"
    ) {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degree)
      li2.units should be theSameInstanceAs degree
      li2.head should equal(la2)
    }

    it("should test using var args | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Float] = Array(1f, 2f, 3f)
      val b: Array[Float] = Array(10f, 20f, 30f)
      val c: Array[Float] = Array(100f, 200f, 300f)

      val li4 = lk.set(a, b, c).withUnits(meter)
      li4.values.length should be(3)
      li4.value(2) should equal(ArrayData(c))
    }
  }

  describe("test floatMatrixKey") {
    val m1: Array[Array[Float]] = Array(Array(1f, 2f, 3f), Array(4f, 5f, 6f), Array(7f, 8f, 9f))
    val m2: Array[Array[Float]] = Array(Array(1f, 2f, 3f, 4f, 5f), Array(10f, 20f, 30f, 40f, 50f))

    val lm1 = MatrixData.fromArrays(m1)
    val lm2 = MatrixData.fromArrays(m2)
    val dk  = FloatMatrixKey.make(s1, encoder)

    it("should work with a single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1)
      di.values should equal(Array(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Array(lm1, lm2)

    it("should work with list and withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn)
      di.units should be theSameInstanceAs encoder
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.head should equal(lm1)
    }

    it("work with varargs | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1, lm2).withUnits(second)
      di.units should be theSameInstanceAs second
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(m1, m2).withUnits(meter)
      di.units should be theSameInstanceAs meter
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test doubleItem") {
    val tval: Double = 123.456
    val lk           = KeyType.DoubleKey.make(s1, encoder)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.set(tval)
      li.values should be(Array(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Array[Double](123.0, 456.0)

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn).withUnits(degree)
      li.units should be(degree)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li = lk.setAll(listIn)
      li.units should be(encoder)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test doubleArrayKey") {
    val a1 = Array[Double](1.0, 2.0, 3.0, 4.0, 5.0)
    val a2 = Array[Double](10.0, 20.0, 30.0, 40.0, 50.0)

    val la1 = ArrayData(a1)
    val la2 = ArrayData(a2)
    val lk  = KeyType.DoubleArrayKey.make(s1, encoder)

    it("should test single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = lk.set(la1)
      di.values should equal(Array(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Array(la1, la2)

    it("should test with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn).withUnits(degree)
      li2.units should be(degree)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li2 = lk.setAll(listIn)
      li2.units should be theSameInstanceAs encoder
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it(
      "should test using one array with and without units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196"
    ) {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degree)
      li2.units should be theSameInstanceAs degree
      li2.head should equal(la2)
    }

    it("should test using var args | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Double] = Array(1, 2, 3)
      val b: Array[Double] = Array(10, 20, 30)
      val c: Array[Double] = Array(100, 200, 300)

      val li4 = lk.set(a, b, c).withUnits(meter)
      li4.values.length should be(3)
      li4.value(2) should equal(ArrayData(c))
    }
  }

  describe("test doubleMatrixKey") {
    val m1: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
    val m2: Array[Array[Double]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

    val lm1 = MatrixData.fromArrays(m1)
    val lm2 = MatrixData.fromArrays(m2)
    val dk  = DoubleMatrixKey.make(s1, encoder)

    it("should work with a single item | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1)
      di.values should equal(Array(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Array(lm1, lm2)

    it("should work with list and withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.setAll(listIn)
      di.units should be theSameInstanceAs encoder
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degree)
      di.units should be theSameInstanceAs degree
      di.head should equal(lm1)
    }

    it("work with varargs | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(lm1, lm2).withUnits(second)
      di.units should be theSameInstanceAs second
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val di = dk.set(m1, m2).withUnits(meter)
      di.units should be theSameInstanceAs meter
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  // DEOPSCSW-282: Add a timestamp Key and Parameter
  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  describe("test UTCTimeItems") {
    val utcTimeValue: UTCTime    = UTCTime.now()
    val utcTimeKey: Key[UTCTime] = KeyType.UTCTimeKey.make(s1)

    it(
      "should allow create a Timestamp parameter from a timestamp key | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-282, DEOPSCSW-661"
    ) {
      val li: Parameter[UTCTime] = utcTimeKey.set(utcTimeValue)
      li.values should be(Array(utcTimeValue))
      li.head should be(utcTimeValue)
      li.get(0).get should equal(utcTimeValue)
    }

    it(
      "and utc must be default Unit | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-282, DEOPSCSW-661, CSW-152"
    ) {
      val li1: Parameter[UTCTime] = utcTimeKey.set(utcTimeValue)
      li1.units should be(utc)

      // must respect overriding also
      val li2: Parameter[UTCTime] = utcTimeKey.set(utcTimeValue).withUnits(NoUnits)
      li2.units should be(NoUnits)
    }

    val listIn = Array[UTCTime](
      UTCTime(UTCTime.now().value.minusSeconds(3600)),
      UTCTime.now(),
      UTCTime(UTCTime.now().value.plusMillis(3600000))
    )

    it(
      "should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-282, DEOPSCSW-661"
    ) {
      val li = utcTimeKey.setAll(listIn).withUnits(second)
      li.units should be(second)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.value(2) should equal(listIn(2))
      li.values should equal(listIn)
    }

    it(
      "should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-282, DEOPSCSW-661"
    ) {
      val li: Parameter[UTCTime] = utcTimeKey.setAll(listIn)
      li.units should be(utc)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.value(2) should equal(listIn(2))
      li.values should equal(listIn)
    }
  }

  // DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
  describe("test TAITimeItems") {
    val taiTimeValue: TAITime    = TAITime.now()
    val taiTimeKey: Key[TAITime] = KeyType.TAITimeKey.make(s1)

    it(
      "should allow create a Timestamp parameter from a timestamp key | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-661"
    ) {
      val li: Parameter[TAITime] = taiTimeKey.set(taiTimeValue)
      li.values should be(Array(taiTimeValue))
      li.head should be(taiTimeValue)
      li.get(0).get should equal(taiTimeValue)
    }

    it(
      "and tai must be default Unit | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-661, CSW-152"
    ) {
      val li1: Parameter[TAITime] = taiTimeKey.set(taiTimeValue)
      li1.units should be(tai)

      // must respect overriding also
      val li2: Parameter[TAITime] = taiTimeKey.set(taiTimeValue).withUnits(NoUnits)
      li2.units should be(NoUnits)
    }

    val listIn = Array[TAITime](
      TAITime(TAITime.now().value.minusSeconds(3600)),
      TAITime.now(),
      TAITime(TAITime.now().value.plusMillis(3600000))
    )

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-661") {
      val li = taiTimeKey.setAll(listIn)
      li.units should be(tai)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.value(2) should equal(listIn(2))
      li.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-661") {
      val li: Parameter[TAITime] = taiTimeKey.setAll(listIn)
      li.units should be(tai)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.value(2) should equal(listIn(2))
      li.values should equal(listIn)
    }
  }

  describe("testing ChoiceItem") {
    it("should allow creating with Choices object | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      // Choices as object with String input
      val choices = Choices.from("A", "B", "C")

      val ci1 = ChoiceKey.make("mode", NoUnits, choices)
      ci1.choices should equal(choices)
      ci1.keyName should be("mode")

      val ci = ci1.set("A")
      ci.head should equal(Choice("A"))
      // Check that non-choice fails
      an[AssertionError] should be thrownBy ci1.set("D")
    }

    it("should allow creating with varargs of Strings | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      // Create directly with keyname, and Choice names
      val ci2 = ChoiceKey.make("test", NoUnits, "A", "B")
      ci2.choices should equal(Choices.from("A", "B"))
      ci2.keyName should be("test")

      val ci = ci2.set("A")
      ci.head should equal(Choice("A"))
      // Check that non-choice fails
      an[AssertionError] should be thrownBy ci2.set("C")
    }

    it(
      "should allow creation with individual Choice items | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196"
    ) {
      // Now create with individual Choice items
      val uninitialized = Choice("uninitialized")
      val ready         = Choice("ready")
      val busy          = Choice("busy")
      val continuous    = Choice("continuous")
      val error         = Choice("error")

      val cmd = ChoiceKey.make("cmd", NoUnits, uninitialized, ready, busy, continuous, error)
      cmd.choices should be(Choices(Set(uninitialized, ready, busy, continuous, error)))

      // setting values
      val ci = cmd.set(ready)
      ci.head should equal(ready)
    }
  }

  describe("Test Coordinate Types") {
    import Angle._
    import Coords._
    it("Should allow coordinate types | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val basePosKey       = CoordKey.make("BasePosition")
      val pm               = ProperMotion(0.5, 2.33)
      val eqCoord          = EqCoord(ra = "12:13:14.15", dec = "-30:31:32.3", frame = FK5, pmx = pm.pmx, pmy = pm.pmy)
      val solarSystemCoord = SolarSystemCoord(Tag("BASE"), Venus)
      val minorPlanetCoord = MinorPlanetCoord(Tag("GUIDER1"), 2000, 90.degree, 2.degree, 100.degree, 1.4, 0.234, 220.degree)
      val cometCoord       = CometCoord(Tag("BASE"), 2000.0, 90.degree, 2.degree, 100.degree, 1.4, 0.234)
      val altAzCoord       = AltAzCoord(Tag("BASE"), 301.degree, 42.5.degree)
      val posParam         = basePosKey.set(eqCoord, solarSystemCoord, minorPlanetCoord, cometCoord, altAzCoord)

      assert(posParam.values.length == 5)
      assert(posParam.values(0) == eqCoord)
      assert(posParam.values(1) == solarSystemCoord)
      assert(posParam.values(2) == minorPlanetCoord)
      assert(posParam.values(3) == cometCoord)
      assert(posParam.values(4) == altAzCoord)
    }
  }

  describe("test stringKey") {

    val keyName   = "stringKey"
    val stringKey = KeyType.StringKey.make(keyName)

    it("should allow single val | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val ii = stringKey.set("First")
      ii.values should be(Array("First"))
      ii.get(0).get should equal("First")
    }

    val listIn = Array("First", "Second")

    it("should work with list, withUnits | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val ii = stringKey.setAll(listIn).withUnits(degree)
      ii.units should be(degree)
      ii.value(1) should equal(listIn(1))
      ii.values should equal(listIn)
    }

    it("should work with list | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196") {
      val ii = stringKey.setAll(listIn)
      ii.units should be(NoUnits)
      ii.value(1) should equal(listIn(1))
      ii.values should equal(listIn)
    }
  }

  // DEOPSCSW-190: Implement Unit Support
  // DEOPSCSW-184: Change configurations - attributes and values
  describe("test setting multiple values") {

    val intKey = KeyType.IntKey.make("test1")
    it(
      "should allow setting a single value | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-190"
    ) {
      val i1 = intKey.set(1)
      assert(i1.values === Array(1))
      assert(i1.units == NoUnits)
      assert(i1(0) == 1)
    }
    it("should allow setting several | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-190") {
      val i1 = intKey.set(1, 3, 5, 7)
      assert(i1.values === Array(1, 3, 5, 7))
      assert(i1.units == NoUnits)
      assert(i1(1) == 3)

      val i2 = intKey.setAll(Array(10, 30, 50, 70)).withUnits(degree)
      assert(i2.values === Array(10, 30, 50, 70))
      assert(i2.units == degree)
      assert(i2(1) == 30)
      assert(i2(3) == 70)

      // to prove that existing Parameter is not mutated and every time `set` is called on key, it creates new Parameter
      assert(i1.values === Array(1, 3, 5, 7))
    }
    it(
      "should also allow setting with sequence | DEOPSCSW-183, DEOPSCSW-185, DEOPSCSW-188, DEOPSCSW-184, DEOPSCSW-196, DEOPSCSW-190"
    ) {
      val s1 = Array(2, 4, 6, 8)
      val i1 = intKey.setAll(s1).withUnits(meter)
      assert(i1.values === s1)
      assert(i1.values.length == s1.length)
      assert(i1.units == meter)
      assert(i1(2) == 6)
    }
  }

  // DEOPS-CSW-MAINT-116: Restrict "[" , "]" and "/" chars in key name of Parameter model
  describe(" Restrict character from key name") {
    it("should not allow [ ]  or / | DEOPS-CSW-MAINT-116") {
      a[IllegalArgumentException] shouldBe thrownBy(KeyType.SolarSystemCoordKey.make("test0]"))
      a[IllegalArgumentException] shouldBe thrownBy(KeyType.BooleanKey.make("test[0"))
      a[IllegalArgumentException] shouldBe thrownBy(KeyType.ChoiceKey.make("test[0]", "1"))
      a[IllegalArgumentException] shouldBe thrownBy(KeyType.ChoiceKey.make("structKey1/someKey/a.b", "1"))
    }
  }
}
