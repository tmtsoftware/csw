package csw.param

import csw.param.UnitsOfMeasure.{degrees, meters, seconds}
import csw.param.parameters._
import csw.param.parameters.arrays._
import csw.param.parameters.matrices._
import csw.param.parameters.primitives._
import org.scalatest.{FunSpec, Matchers}

/**
 * TMT Source Code: 7/7/16.
 */
class ItemsTests extends FunSpec with Matchers {

  private val s1: String = "encoder"
  private val s2: String = "filter"

  describe("basic key tests") {
    val k1: IntKey = IntKey(s1)
    val k2: IntKey = IntKey(s2)
    it("should have correct name") {
      assert(k1.keyName.equals(s1))
    }

    val k3: IntKey = IntKey(s1)

    it("should have equality based on name") {
      assert(k3 == k1)
      assert(k3 != k2)
      assert(k1 != k2)
    }
  }

  describe("test booleanKey") {

    val tval = false
    val bk   = Keys.Boolean(s1)

    it("should allow single val") {
      val ii = bk.set(tval)
      ii.values should be(Vector(tval))
      ii.get(0).get should equal(tval)
    }

    val listIn = Vector(false, true)

    it("should work with list, withUnits") {
      val ii = bk.set(listIn).withUnits(degrees)
      ii.units should be(degrees)
      ii.value(1) should equal(listIn(1))
      ii.values should equal(listIn)
    }

    it("should work with list, units") {
      val ii = bk.set(listIn, degrees)
      ii.units should be(degrees)
      ii.value(1) should equal(listIn(1))
      ii.values should equal(listIn)
    }
  }

  describe("test ByteArrayKey") {
    val a1: Array[Byte] = Array[Byte](1, 2, 3, 4, 5)
    val a2: Array[Byte] = Array[Byte](10, 20, 30, 40, 50)

    val la1 = ByteArray(a1)
    val la2 = ByteArray(a2)
    val lk  = ByteArrayKey(s1)

    it("should test single item") {
      val di: ByteArrayParameter = lk.set(la1)
      di.values should equal(Vector(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Vector(la1, la2)

    it("should test with list, withUnits") {
      val li2: ByteArrayParameter = lk.set(listIn).withUnits(UnitsOfMeasure.degrees)
      li2.units should be(degrees)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list, units") {
      val li2 = lk.set(listIn, degrees)
      li2.units should be theSameInstanceAs degrees
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test using one array with and without units") {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degrees)
      li2.units should be theSameInstanceAs degrees
      li2.head should equal(la2)
    }

    it("should test using var args") {
      val li3: ByteArrayParameter = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Byte] = Array[Byte](1, 2, 3)
      val b: Array[Byte] = Array[Byte](10, 20, 30)
      val c: Array[Byte] = Array[Byte](100, 101, 102)

      val li4: ByteArrayParameter = lk.set(a, b, c).withUnits(meters)
      li4.values.size should be(3)
      li4.value(2) should equal(ByteArray(c))
    }
  }

  describe("test byteMatrixKey") {
    val m1: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3), Array[Byte](4, 5, 6), Array[Byte](7, 8, 9))
    val m2: Array[Array[Byte]] = Array(Array[Byte](1, 2, 3, 4, 5), Array[Byte](10, 20, 30, 40, 50))

    val lm1 = ByteMatrix(m1)
    val lm2 = ByteMatrix(m2)
    val dk  = ByteMatrixKey(s1)

    it("should work with a single item") {
      val di = dk.set(lm1)
      di.values should equal(Vector(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Vector(lm1, lm2)

    it("should work with list and withUnits") {
      val di = dk.set(listIn).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list and units") {
      val di = dk.set(listIn, degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.head should equal(lm1)
    }

    it("work with varargs") {
      val di = dk.set(lm1, lm2).withUnits(seconds)
      di.units should be theSameInstanceAs seconds
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays") {
      val di = dk.set(m1, m2).withUnits(meters)
      di.units should be theSameInstanceAs meters
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test charItem") {
    val tval = 'K'
    val lk   = CharKey(s1)

    it("should allow single val") {
      val li = lk.set(tval)
      li.values should be(Vector(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Vector[Char]('K', 'G')

    it("should work with list, withUnits") {
      val li = lk.set(listIn).withUnits(degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list, units") {
      val li = lk.set(listIn, degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test doubleItem") {
    val tval: Double = 123.456
    val lk           = DoubleKey(s1)

    it("should allow single val") {
      val li = lk.set(tval)
      li.values should be(Vector(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Vector[Double](123.0, 456.0)

    it("should work with list, withUnits") {
      val li = lk.set(listIn).withUnits(degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list, units") {
      val li = lk.set(listIn, degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test doubleArrayKey") {
    val a1 = Array[Double](1.0, 2.0, 3.0, 4.0, 5.0)
    val a2 = Array[Double](10.0, 20.0, 30.0, 40.0, 50.0)

    val la1 = DoubleArray(a1)
    val la2 = DoubleArray(a2)
    val lk  = DoubleArrayKey(s1)

    it("should test single item") {
      val di = lk.set(la1)
      di.values should equal(Vector(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Vector(la1, la2)

    it("should test with list, withUnits") {
      val li2 = lk.set(listIn).withUnits(UnitsOfMeasure.degrees)
      li2.units should be(degrees)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list, units") {
      val li2 = lk.set(listIn, degrees)
      li2.units should be theSameInstanceAs degrees
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test using one array with and without units") {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degrees)
      li2.units should be theSameInstanceAs degrees
      li2.head should equal(la2)
    }

    it("should test using var args") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Double] = Array(1, 2, 3)
      val b: Array[Double] = Array(10, 20, 30)
      val c: Array[Double] = Array(100, 200, 300)

      val li4 = lk.set(a, b, c).withUnits(meters)
      li4.values.size should be(3)
      li4.value(2) should equal(DoubleArray(c))
    }
  }

  describe("test doubleMatrixKey") {
    val m1: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
    val m2: Array[Array[Double]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

    val lm1 = DoubleMatrix(m1)
    val lm2 = DoubleMatrix(m2)
    val dk  = DoubleMatrixKey(s1)

    it("should work with a single item") {
      val di = dk.set(lm1)
      di.values should equal(Vector(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Vector(lm1, lm2)

    it("should work with list and withUnits") {
      val di = dk.set(listIn).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list and units") {
      val di = dk.set(listIn, degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.head should equal(lm1)
    }

    it("work with varargs") {
      val di = dk.set(lm1, lm2).withUnits(seconds)
      di.units should be theSameInstanceAs seconds
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays") {
      val di = dk.set(m1, m2).withUnits(meters)
      di.units should be theSameInstanceAs meters
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test floatItem") {
    val tval: Float = 123.456f
    val lk          = FloatKey(s1)

    it("should allow single val") {
      val li = lk.set(tval)
      li.values should be(Vector(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Vector[Float](123.0f, 456.0f)

    it("should work with list, withUnits") {
      val li = lk.set(listIn).withUnits(degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list, units") {
      val li = lk.set(listIn, degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test floatArrayKey") {
    val a1 = Array[Float](1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
    val a2 = Array[Float](10.0f, 20.0f, 30.0f, 40.0f, 50.0f)

    val la1 = FloatArray(a1)
    val la2 = FloatArray(a2)
    val lk  = FloatArrayKey(s1)

    it("should test single item") {
      val di = lk.set(la1)
      di.values should equal(Vector(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Vector(la1, la2)

    it("should test with list, withUnits") {
      val li2 = lk.set(listIn).withUnits(UnitsOfMeasure.degrees)
      li2.units should be(degrees)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list, units") {
      val li2 = lk.set(listIn, degrees)
      li2.units should be theSameInstanceAs degrees
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test using one array with and without units") {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degrees)
      li2.units should be theSameInstanceAs degrees
      li2.head should equal(la2)
    }

    it("should test using var args") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Float] = Array(1f, 2f, 3f)
      val b: Array[Float] = Array(10f, 20f, 30f)
      val c: Array[Float] = Array(100f, 200f, 300f)

      val li4 = lk.set(a, b, c).withUnits(meters)
      li4.values.size should be(3)
      li4.value(2) should equal(FloatArray(c))
    }
  }

  describe("test floatMatrixKey") {
    val m1: Array[Array[Float]] = Array(Array(1f, 2f, 3f), Array(4f, 5f, 6f), Array(7f, 8f, 9f))
    val m2: Array[Array[Float]] = Array(Array(1f, 2f, 3f, 4f, 5f), Array(10f, 20f, 30f, 40f, 50f))

    val lm1 = FloatMatrix(m1)
    val lm2 = FloatMatrix(m2)
    val dk  = FloatMatrixKey(s1)

    it("should work with a single item") {
      val di = dk.set(lm1)
      di.values should equal(Vector(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Vector(lm1, lm2)

    it("should work with list and withUnits") {
      val di = dk.set(listIn).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list and units") {
      val di = dk.set(listIn, degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.head should equal(lm1)
    }

    it("work with varargs") {
      val di = dk.set(lm1, lm2).withUnits(seconds)
      di.units should be theSameInstanceAs seconds
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays") {
      val di = dk.set(m1, m2).withUnits(meters)
      di.units should be theSameInstanceAs meters
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test IntItem") {
    val tval: Int = 1234
    val lk        = IntKey(s1)

    it("should allow single val") {
      val li = lk.set(tval)
      li.values should be(Vector(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Vector[Int](123, 456)

    it("should work with list, withUnits") {
      val li = lk.set(listIn).withUnits(degrees)
      li.units should be(degrees)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list, units") {
      val li = lk.set(listIn, degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test intArrayKey") {
    val a1 = Array[Int](1, 2, 3, 4, 5)
    val a2 = Array[Int](10, 20, 30, 40, 50)

    val la1 = IntArray(a1)
    val la2 = IntArray(a2)
    val lk  = IntArrayKey(s1)

    it("should test single item") {
      val di = lk.set(la1)
      di.values should equal(Vector(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Vector(la1, la2)

    it("should test with list, withUnits") {
      val li2 = lk.set(listIn).withUnits(UnitsOfMeasure.degrees)
      li2.units should be(degrees)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list, units") {
      val li2 = lk.set(listIn, degrees)
      li2.units should be theSameInstanceAs degrees
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test using one array with and without units") {
      var li2 = lk.set(a1) // Uses implicit to create from int array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degrees)
      li2.units should be theSameInstanceAs degrees
      li2.head should equal(la2)
    }

    it("should test using var args") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Int] = Array(1, 2, 3)
      val b: Array[Int] = Array(10, 20, 30)
      val c: Array[Int] = Array(100, 200, 300)

      val li4 = lk.set(a, b, c).withUnits(meters)
      li4.values.size should be(3)
      li4.value(2) should equal(IntArray(c))
    }
  }

  describe("test intMatrixKey") {
    val m1: Array[Array[Int]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
    val m2: Array[Array[Int]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

    val lm1 = IntMatrix(m1)
    val lm2 = IntMatrix(m2)
    val dk  = IntMatrixKey(s1)

    it("should work with a single item") {
      val di = dk.set(lm1)
      di.values should equal(Vector(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Vector(lm1, lm2)

    it("should work with list and withUnits") {
      val di = dk.set(listIn).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list and units") {
      val di = dk.set(listIn, degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.head should equal(lm1)
    }

    it("work with varargs") {
      val di = dk.set(lm1, lm2).withUnits(seconds)
      di.units should be theSameInstanceAs seconds
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays") {
      val di = dk.set(m1, m2).withUnits(meters)
      di.units should be theSameInstanceAs meters
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test longKey") {
    val lval        = 1234L
    val lk: LongKey = LongKey(s1)

    it("should allow single val") {
      val li = lk.set(lval)
      li.values should be(Vector(lval))
      li.get(0).get should equal(lval)
    }

    val listIn = Vector[Long](123L, 456L)

    it("should work with list, withUnits") {
      val li = lk.set(listIn).withUnits(degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list, units") {
      val li = lk.set(listIn, degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test LongArrayKey") {
    val a1: Array[Long] = Array(1, 2, 3, 4, 5)
    val a2: Array[Long] = Array(10, 20, 30, 40, 50)

    val la1: LongArray   = LongArray(a1)
    val la2: LongArray   = LongArray(a2)
    val lk: LongArrayKey = LongArrayKey(s1)

    it("should test single item") {
      val di: LongArrayParameter = lk.set(la1)
      di.values should equal(Vector(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Vector(la1, la2)

    it("should test with list, withUnits") {
      val li2: LongArrayParameter = lk.set(listIn).withUnits(UnitsOfMeasure.degrees)
      li2.units should be(degrees)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list, units") {
      val li2 = lk.set(listIn, degrees)
      li2.units should be theSameInstanceAs degrees
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test using one array with and without units") {
      var li2 = lk.set(a1) // Uses implicit to create from long array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degrees)
      li2.units should be theSameInstanceAs degrees
      li2.head should equal(la2)
    }

    it("should test using var args") {
      val li3: LongArrayParameter = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Long] = Array(1, 2, 3)
      val b: Array[Long] = Array(10, 20, 30)
      val c: Array[Long] = Array(100, 200, 300)

      val li4: LongArrayParameter = lk.set(a, b, c).withUnits(meters)
      li4.values.size should be(3)
      li4.value(2) should equal(LongArray(c))
    }
  }

  describe("test longMatrixKey") {
    val m1: Array[Array[Long]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
    val m2: Array[Array[Long]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

    val lm1: LongMatrix   = LongMatrix(m1)
    val lm2: LongMatrix   = LongMatrix(m2)
    val dk: LongMatrixKey = LongMatrixKey(s1)

    it("should work with a single item") {
      val di = dk.set(lm1)
      di.values should equal(Vector(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Vector(lm1, lm2)

    it("should work with list and withUnits") {
      val di = dk.set(listIn).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list and units") {
      val di = dk.set(listIn, degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.head should equal(lm1)
    }

    it("work with varargs") {
      val di = dk.set(lm1, lm2).withUnits(seconds)
      di.units should be theSameInstanceAs seconds
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays") {
      val di = dk.set(m1, m2).withUnits(meters)
      di.units should be theSameInstanceAs meters
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("test ShortItem") {
    val tval: Short = 1234
    val lk          = ShortKey(s1)

    it("should allow single val") {
      val li = lk.set(tval)
      li.values should be(Vector(tval))
      li.head should be(tval)
      li.get(0).get should equal(tval)
    }

    val listIn = Vector[Short](123, 456)

    it("should work with list, withUnits") {
      val li = lk.set(listIn).withUnits(degrees)
      li.units should be(degrees)
      li.value(0) should equal(listIn(0))
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }

    it("should work with list, units") {
      val li = lk.set(listIn, degrees)
      li.units should be(degrees)
      li.value(1) should equal(listIn(1))
      li.values should equal(listIn)
    }
  }

  describe("test shortArrayKey") {
    val a1 = Array[Short](1, 2, 3, 4, 5)
    val a2 = Array[Short](10, 20, 30, 40, 50)

    val la1 = ShortArray(a1)
    val la2 = ShortArray(a2)
    val lk  = ShortArrayKey(s1)

    it("should test single item") {
      val di = lk.set(la1)
      di.values should equal(Vector(la1))
      di.head should be(la1)
      di.get(0).get should equal(la1)
    }

    val listIn = Vector(la1, la2)

    it("should test with list, withUnits") {
      val li2 = lk.set(listIn).withUnits(UnitsOfMeasure.degrees)
      li2.units should be(degrees)
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test with list, units") {
      val li2 = lk.set(listIn, degrees)
      li2.units should be theSameInstanceAs degrees
      li2.value(1) should equal(listIn(1))
      li2.values should equal(listIn)
    }

    it("should test using one array with and without units") {
      var li2 = lk.set(a1) // Uses implicit to create from int array
      li2.head should equal(la1)
      li2 = lk.set(a2).withUnits(degrees)
      li2.units should be theSameInstanceAs degrees
      li2.head should equal(la2)
    }

    it("should test using var args") {
      val li3 = lk.set(la1, la2)
      li3.value(1) should equal(la2)
      li3.values should equal(listIn)

      val a: Array[Short] = Array[Short](1, 2, 3)
      val b: Array[Short] = Array[Short](10, 20, 30)
      val c: Array[Short] = Array[Short](100, 200, 300)

      val li4 = lk.set(a, b, c).withUnits(meters)
      li4.values.size should be(3)
      li4.value(2) should equal(ShortArray(c))
    }
  }

  describe("test shortMatrixKey") {
    val m1: Array[Array[Short]] = Array(Array[Short](1, 2, 3), Array[Short](4, 5, 6), Array[Short](7, 8, 9))
    val m2: Array[Array[Short]] = Array(Array[Short](1, 2, 3, 4, 5), Array[Short](10, 20, 30, 40, 50))

    val lm1 = ShortMatrix(m1)
    val lm2 = ShortMatrix(m2)
    val dk  = ShortMatrixKey(s1)

    it("should work with a single item") {
      val di = dk.set(lm1)
      di.values should equal(Vector(lm1))
      di.head should equal(lm1)
      di.get(0).get should equal(lm1)
    }

    val listIn = Vector(lm1, lm2)

    it("should work with list and withUnits") {
      val di = dk.set(listIn).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("should work with list and units") {
      val di = dk.set(listIn, degrees)
      di.units should be theSameInstanceAs degrees
      di.value(1) should equal(listIn(1))
      di.values should equal(listIn)
    }

    it("work with one matrix without and with units") {
      var di = dk.set(m1) // This is an implicit
      di.head should equal(lm1)
      di = dk.set(m1).withUnits(degrees)
      di.units should be theSameInstanceAs degrees
      di.head should equal(lm1)
    }

    it("work with varargs") {
      val di = dk.set(lm1, lm2).withUnits(seconds)
      di.units should be theSameInstanceAs seconds
      di.value(1) should equal(lm2)
      di.values should equal(listIn)
    }

    it("work with varargs as arrays") {
      val di = dk.set(m1, m2).withUnits(meters)
      di.units should be theSameInstanceAs meters
      di.value(0) should equal(lm1)
      di.values should equal(listIn)
    }
  }

  describe("testing ChoiceItem") {
    it("should allow creating with Choices object") {
      // Choices as object with String input
      val choices = Choices.from("A", "B", "C")

      val ci1 = ChoiceKey("mode", choices)
      ci1.choices should equal(choices)
      ci1.keyName should be("mode")

      val ci = ci1.set("A")
      ci.head should equal(Choice("A"))
      // Check that non-choice fails
      an[AssertionError] should be thrownBy (ci1.set("D"))
    }

    it("should allow creating with varargs of Strings") {
      // Create directly with keyname, and Choice names
      val ci2 = ChoiceKey("test", "A", "B")
      ci2.choices should equal(Choices.from("A", "B"))
      ci2.keyName should be("test")

      val ci = ci2.set("A")
      ci.head should equal(Choice("A"))
      // Check that non-choice fails
      an[AssertionError] should be thrownBy (ci2.set("C"))
    }

    it("should allow creation with individual Choice items") {
      // Now create with individual Choice items
      val uninitialized = Choice("uninitialized")
      val ready         = Choice("ready")
      val busy          = Choice("busy")
      val continuous    = Choice("continuous")
      val error         = Choice("error")

      val cmd = ChoiceKey("cmd", uninitialized, ready, busy, continuous, error)
      cmd.choices should be(Choices(Set(uninitialized, ready, busy, continuous, error)))

      // setting values
      val ci = cmd.set(ready)
      ci.head should equal(ready)
    }
  }

  describe("testing StructItem") {
    it("should allow creating one of them") {
      val skey = StructKey("ao.sys.oiwfs")

      val ra    = StringKey("ra")
      val dec   = StringKey("dec")
      val epoch = DoubleKey("epoch")
      val sc1   = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val citem = skey.set(sc1)

      println(citem)

    }
  }

  describe("testing StructItem") {
    it("should allow creating Struct items") {
      val skey = StructKey("myStruct")

      val ra    = StringKey("ra")
      val dec   = StringKey("dec")
      val epoch = DoubleKey("epoch")
      val sc1   = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val citem = skey.set(sc1)

      assert(citem.size == 1)
      assert(citem.head.size == 3)
      assert(citem.head.get(ra).head.head == "12:13:14.1")
      assert(citem.head.get(dec).head.head == "32:33:34.4")
      assert(citem.head.get(epoch).head.head == 1950.0)

    }
  }
}
