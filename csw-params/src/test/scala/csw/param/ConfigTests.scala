package csw.param

import csw.param.Events.StatusEvent
import csw.param.Parameters._
import csw.param.UnitsOfMeasure.{degrees, meters, _}
import csw.param.parameters.arrays._
import csw.param.parameters.matrices._
import csw.param.parameters.primitives._
import org.scalatest.FunSpec

import scala.collection.immutable.Vector
import scala.util.Try

//noinspection ComparingUnrelatedTypes,ScalaUnusedSymbol
class ConfigTests extends FunSpec {

  private val s1: String = "encoder"
  private val s2: String = "filter"

  private val ck1 = "wfos.prog.cloudcover"
  private val ck3 = "wfos.red.detector"

  private val commandInfo: CommandInfo = "Obs001"

  describe("Basic key tests") {
    val k1 = IntKey(s1)
    val k2 = StringKey(s2)

    it("Should be constructed properly") {
      assert(k1.keyName eq s1)
    }

    it("Should use set properly") {
      val i: IntParameter = k1.set(22)
      // Check that name and value are set
      assert(i.keyName eq s1)
      assert(i.values == Vector(22))
      assert(i.head == 22)

      assert(k2.keyName eq s2)
      val j: StringParameter = k2.set("Bob").withUnits(UnitsOfMeasure.meters)
      assert(j.values == Vector("Bob"))
      assert(j.units == meters)
    }

    it("Should support equality of keys") {
      val k3 = IntKey(s1)
      assert(k3 == k1)
      assert(k3 != k2)
      assert(k1 != k2)
    }
  }

  describe("SC Basic Tests") {
    val k1    = IntKey("encoder")
    val k2    = StringKey("stringThing")
    val k2bad = IntKey("stringThing")
    val k3    = IntKey("notUsed")

    it("Should allow adding keys using single set") {
      val i1  = k1.set(22)
      val i2  = k2.set("A")
      val sc1 = Setup(commandInfo, ck3).add(i1).add(i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1.exists(k2bad))

      // Validation of the correct type needs to be done with concrete types, outside the generic API!
      assert(Try(sc1(k1)).isSuccess)
      assert(Try(sc1(k2)).isSuccess)
      assert(Try(sc1(k2bad)).isFailure)
      assert(Try(sc1.get(k2bad).get).isFailure)

      assert(sc1.get(k1).head == i1)
      assert(sc1.get(k2).head == i2)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("Should allow setting with units") {
      var sc1 = Setup(commandInfo, ck1)
      sc1 = sc1.madd(k1.set(22).withUnits(degrees), k2.set("B"))
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1.get(k1).map(_.units == degrees).get)
      assert(sc1.get(k2).get.units == NoUnits)
    }

    it("Should allow apply which returns values") {
      var sc1 = Setup(commandInfo, ck1)
      sc1 = sc1.madd(k1.set(22).withUnits(degrees), k2.set("C"))

      val v1: IntParameter    = sc1(k1)
      val v2: StringParameter = sc1(k2)
      assert(sc1.get(k1).isDefined)
      assert(sc1.get(k2).isDefined)
      assert(v1.values == Vector(22))
      assert(v2.values == Vector("C"))
      assert(sc1(k2)(0) == "C")
    }

    it("should update for the same key with set") {
      var sc1 = Setup(commandInfo, ck1)
      sc1 = sc1.add(k2.set("D"))
      assert(sc1.exists(k2))
      assert(sc1(k2).values == Vector("D"))

      sc1 = sc1.add(k2.set("E").withUnits(meters))
      assert(sc1.exists(k2))
      assert(sc1(k2).units == meters)
      assert(sc1(k2).values == Vector("E"))
    }
  }

  describe("Checking key updates") {
    val k1: IntKey = IntKey("atest")

    it("Should allow updates") {
      val i1 = k1.set(22)
      assert(i1.head == 22)
      assert(i1.units == NoUnits)
      val i2 = k1.set(33)
      assert(i2.head == 33)
      assert(i2.units == NoUnits)

      var sc = Setup(commandInfo, ck1).add(i1)
      // Use option
      assert(sc.get(k1).get == i1)
      assert(sc.get(k1).get.head == 22)
      // Use direct
      assert(sc(k1).values == Vector(22))
      assert(sc(k1).value(0) == 22)
      sc = sc.add(i2)
      assert(sc(k1).head == 33)
    }
  }

  describe("Test Long") {
    it("should allow setting from Long") {
      val tval = 1234L
      val k1   = LongKey(s1)
      val i1   = k1.set(tval)
      assert(i1.values == Vector(tval))
      assert(i1.values(0) == tval)
      assert(i1.head == tval)

      val tval2 = 4567L
      val k2    = LongKey(s1)
      val i2    = k2.set(tval2)
      assert(i2.values == Vector(tval2))
    }
  }

  describe("StatusEvent Test") {

    val k1 = IntKey("encoder")
    val k2 = IntKey("windspeed")
    val k3 = IntKey("notUsed")

    it("Should allow adding keys") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val sc1 = StatusEvent(ck3).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("Should allow setting") {
      var sc1 = StatusEvent(ck1)
      sc1 = sc1.add(k1.set(22)).add(k2.set(44))
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
    }

    it("Should allow apply") {
      var sc1 = StatusEvent(ck1)
      sc1 = sc1.add(k1.set(22)).add(k2.set(44))

      val v1 = sc1(k1)
      val v2 = sc1(k2)
      assert(sc1.get(k1).isDefined)
      assert(sc1.get(k2).isDefined)
      assert(v1.values == Vector(22))
      assert(v2.values(0) == 44)
    }

    it("should update for the same key with set") {
      var sc1 = StatusEvent(ck1)
      sc1 = sc1.add(k2.set(22))
      assert(sc1.exists(k2))
      assert(sc1(k2).values == Vector(22))

      sc1 = sc1.add(k2.set(33).withUnits(meters))
      assert(sc1.exists(k2))
      assert(sc1(k2).units == meters)
      assert(sc1(k2).values == Vector(33))
    }
  }

  describe("OC Test") {

    val k1 = IntKey("repeat")
    val k2 = IntKey("expTime")
    it("Should allow adding keys") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val oc1 = Observe(commandInfo, ck3).add(i1).add(i2)
      assert(oc1.size == 2)
      assert(oc1.exists(k1))
      assert(oc1.exists(k2))
      assert(oc1(k1).head == 22)
      assert(oc1.get(k2).get.head == 44)
    }

    it("Should allow setting") {
      var oc1 = Observe(commandInfo, ck1)
      oc1 = oc1.add(k1.set(22)).add(k2.set(44))
      assert(oc1.size == 2)
      assert(oc1.exists(k1))
      assert(oc1.exists(k2))
    }

    it("Should allow apply") {
      var oc1 = Observe(commandInfo, ck1)
      oc1 = oc1.add(k1.set(22)).add(k2.set(44))

      val v1 = oc1(k1)
      val v2 = oc1(k2)
      assert(oc1.get(k1).isDefined)
      assert(oc1.get(k2).isDefined)
      assert(v1.values == Vector(22))
      assert(v2.head == 44)
    }

    it("should update for the same key with set") {
      var oc1 = Observe(commandInfo, ck1)
      oc1 = oc1.add(k2.set(22))
      assert(oc1.exists(k2))
      assert(oc1(k2).values == Vector(22))

      oc1 = oc1.add(k2.set(33))
      assert(oc1.exists(k2))
      assert(oc1(k2).values == Vector(33))
    }

    it("should update for the same key with add") {
      var oc1 = Observe(commandInfo, ck1)
      oc1 = oc1.add(k2.set(22).withUnits(NoUnits))
      assert(oc1.exists(k2))
      assert(oc1(k2).values == Vector(22))

      oc1 = oc1.add(k2.set(33).withUnits(NoUnits))
      assert(oc1.exists(k2))
      assert(oc1(k2).values == Vector(33))
    }
  }

  describe("test setting multiple values") {
    val t1 = IntKey("test1")
    it("should allow setting a single value") {
      val i1 = t1.set(1)
      assert(i1.values == Vector(1))
      assert(i1.units == NoUnits)
      assert(i1(0) == 1)
    }
    it("should allow setting several") {
      val i1 = t1.set(1, 3, 5, 7)
      assert(i1.values == Vector(1, 3, 5, 7))
      assert(i1.units == NoUnits)
      assert(i1(1) == 3)

      val i2 = t1.set(Vector(10, 30, 50, 70)).withUnits(degrees)
      assert(i2.values == Vector(10, 30, 50, 70))
      assert(i2.units == degrees)
      assert(i2(1) == 30)
      assert(i2(3) == 70)
    }
    it("should also allow setting with sequence") {
      val s1 = Vector(2, 4, 6, 8)
      val i1 = t1.set(s1).withUnits(meters)
      assert(i1.values == s1)
      assert(i1.values.size == s1.size)
      assert(i1.units == meters)
      assert(i1(2) == 6)
    }
  }

  describe("testing for getting typed items") {
    val t1  = IntKey("test1")
    val sc1 = Setup(commandInfo, ck1).add(t1.set(Vector(22), degrees))

    val item: Option[IntParameter] = sc1.get(t1) // Works now!
    val itm: IntParameter          = item.get
    assert(itm.units == UnitsOfMeasure.degrees)
    val i: Int = itm(0)
    assert(i == 22)
    val i2: Int = itm.head
    assert(i2 == i)
    val i3: Int = sc1(t1).head
    assert(i3 == i)
  }

  describe("Checking for item types in configs") {
    val k1: IntKey    = IntKey("itest")
    val k2: DoubleKey = DoubleKey("dtest")
    val k3: StringKey = StringKey("stest")

    val i1 = k1.set(1, 2, 3).withUnits(UnitsOfMeasure.degrees)
    val i2 = k2.set(1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)
    val i3 = k3.set("A", "B", "C")

    it("Should get as IntItem") {
      val sc = Setup(commandInfo, ck1).add(i1).add(i2).add(i3)

      val out1: Option[IntParameter]    = sc.get(k1)
      val out2: Option[DoubleParameter] = sc.get(k2)
      val out3: Option[StringParameter] = sc.get(k3)

      assert(out1.get.values === Vector(1, 2, 3))
      assert(out2.get.values === Vector(1.0, 2.0, 3.0))
      assert(out3.get.values === Vector("A", "B", "C"))
    }
  }

  describe("Check for multi-add") {
    val k1: IntKey    = IntKey("itest")
    val k2: DoubleKey = DoubleKey("dtest")
    val k3: StringKey = StringKey("stest")

    val i1 = k1.set(1, 2, 3).withUnits(UnitsOfMeasure.degrees)
    val i2 = k2.set(1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)
    val i3 = k3.set("A", "B", "C")

    it("Should allow vararg add") {
      val sc = Setup(commandInfo, ck1).madd(i1, i2, i3)
      assert(sc.size == 3)
      assert(sc.exists(k1))
      assert(sc.exists(k2))
      assert(sc.exists(k3))
      info("SC: " + sc)

    }
  }

  describe("Should work with remove") {
    val k1 = IntKey("itest")
    val k2 = DoubleKey("dtest")
    val k3 = StringKey("stest")
    val k4 = LongArrayKey("lartest")

    val i1 = k1.set(1, 2, 3).withUnits(UnitsOfMeasure.degrees)
    val i2 = k2.set(1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)
    val i3 = k3.set("A", "B", "C")
    val i4 = k4.set(LongArray(Array.fill[Long](100)(10)), LongArray(Array.fill[Long](100)(100)))

    it("Should allow removing one at a time") {
      var sc1 = Setup(commandInfo, ck1).madd(i1, i2, i3, i4)
      assert(sc1.size == 4)
      assert(sc1.get(k1).isDefined)
      assert(sc1.get(k2).isDefined)
      assert(sc1.get(k3).isDefined)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(k1)
      assert(sc1.size == 3)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isDefined)
      assert(sc1.get(k3).isDefined)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(k2)
      assert(sc1.size == 2)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isDefined)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(k3)
      assert(sc1.size == 1)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isDefined)

      // Should allow removing non-existent
      sc1 = sc1.remove(k3)
      assert(sc1.size == 1)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(k4)
      assert(sc1.size == 0)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isEmpty)

      // Add allows re-adding
      sc1 = sc1.add(i4)
      assert(sc1.size == 1)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isDefined)
    }
  }

  describe("should work with remove by item") {
    val k1 = IntKey("itest")
    val k2 = DoubleKey("dtest")
    val k3 = StringKey("stest")
    val k4 = LongArrayKey("lartest")

    val i1  = k1.set(1, 2, 3).withUnits(UnitsOfMeasure.degrees)
    val i11 = k1.set(1, 2, 3).withUnits(UnitsOfMeasure.degrees) // This is here to see if it is checking equality or address
    val i2  = k2.set(1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)
    val i3  = k3.set("A", "B", "C")
    val i4  = k4.set(LongArray(Array.fill[Long](100)(10)), LongArray(Array.fill[Long](100)(100)))
    val i5  = k1.set(22) // This is not added for testing not present removal

    it("Should allow removing one at a time") {
      var sc1 = Setup(commandInfo, ck1).madd(i1, i2, i3, i4)
      assert(sc1.size == 4)
      assert(sc1.get(k1).isDefined)
      assert(sc1.get(k2).isDefined)
      assert(sc1.get(k3).isDefined)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(i1)
      assert(sc1.size == 3)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isDefined)
      assert(sc1.get(k3).isDefined)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(i2)
      assert(sc1.size == 2)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isDefined)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(i3)
      assert(sc1.size == 1)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isDefined)

      // Should allow removing non-existent
      sc1 = sc1.remove(i5)
      assert(sc1.size == 1)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isDefined)

      sc1 = sc1.remove(i4)
      assert(sc1.size == 0)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isEmpty)

      // Add allows re-adding
      sc1 = sc1.add(i1)
      assert(sc1.size == 1)
      assert(sc1.get(k1).isDefined)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isEmpty)

      // Here we are removing with an item identical ot i1, but different address to check
      // if it is removing on address or contents (should be latter)
      sc1 = sc1.remove(i11)
      assert(sc1.size == 0)
      assert(sc1.get(k1).isEmpty)
      assert(sc1.get(k2).isEmpty)
      assert(sc1.get(k3).isEmpty)
      assert(sc1.get(k4).isEmpty)
    }
  }

  describe("Array-based long array equality") {
    val k1 = LongArrayKey("myLongArray")
    val m1 = LongArray(Array(1, 2, 3))
    val m2 = LongArray(Array(1, 2, 3))
    val m3 = LongArray(Array(1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }
    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }
    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }

  }

  describe("Array-based long matrix equality") {
    val k1 = LongMatrixKey("myMatrix")
    val m1 = LongMatrix(Array(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12)))
    val m2 = LongMatrix(Array(Array(1, 2, 3), Array(2, 3, 6), Array(4, 6, 12)))
    val m3 = LongMatrix(Array(Array(1, 2, 3), Array(2, 3, 6), Array(0, 6, 12))) // Note one value different
    val m4 = LongMatrix(Array(Array(1, 0, 0), Array(0, 1, 0), Array(0, 0, 1)))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based byte array equality") {
    val k1 = ByteArrayKey("myByteArray")
    val m1 = ByteArray(Array[Byte](1, 2, 3))
    val m2 = ByteArray(Array[Byte](1, 2, 3))
    val m3 = ByteArray(Array[Byte](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based byte matrix equality") {
    val k1 = ByteMatrixKey("myMatrix")
    val m1 = ByteMatrix(Array(Array[Byte](1, 2, 3), Array[Byte](2, 3, 6), Array[Byte](4, 6, 12)))
    val m2 = ByteMatrix(Array(Array[Byte](1, 2, 3), Array[Byte](2, 3, 6), Array[Byte](4, 6, 12)))
    val m3 = ByteMatrix(Array(Array[Byte](1, 2, 3), Array[Byte](2, 3, 6), Array[Byte](0, 6, 12))) // Note one value different
    val m4 = ByteMatrix(Array(Array[Byte](1, 0, 0), Array[Byte](0, 1, 0), Array[Byte](0, 0, 1)))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based double array equality") {
    val k1 = DoubleArrayKey("myArray")
    val m1 = DoubleArray(Array[Double](1, 2, 3))
    val m2 = DoubleArray(Array[Double](1, 2, 3))
    val m3 = DoubleArray(Array[Double](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based double matrix equality") {
    val k1 = DoubleMatrixKey("myMatrix")
    val m1 = DoubleMatrix(Array(Array[Double](1, 2, 3), Array[Double](2, 3, 6), Array[Double](4, 6, 12)))
    val m2 = DoubleMatrix(Array(Array[Double](1, 2, 3), Array[Double](2, 3, 6), Array[Double](4, 6, 12)))
    val m3 = DoubleMatrix(Array(Array[Double](1, 2, 3), Array[Double](2, 3, 6), Array[Double](0, 6, 12))) // Note one value different
    val m4 = DoubleMatrix(Array(Array[Double](1, 0, 0), Array[Double](0, 1, 0), Array[Double](0, 0, 1)))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based float array equality") {
    val k1 = FloatArrayKey("myArray")
    val m1 = FloatArray(Array[Float](1, 2, 3))
    val m2 = FloatArray(Array[Float](1, 2, 3))
    val m3 = FloatArray(Array[Float](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based float matrix equality") {
    val k1 = FloatMatrixKey("myMatrix")
    val m1 = FloatMatrix(Array(Array[Float](1, 2, 3), Array[Float](2, 3, 6), Array[Float](4, 6, 12)))
    val m2 = FloatMatrix(Array(Array[Float](1, 2, 3), Array[Float](2, 3, 6), Array[Float](4, 6, 12)))
    val m3 = FloatMatrix(Array(Array[Float](1, 2, 3), Array[Float](2, 3, 6), Array[Float](0, 6, 12))) // Note one value different
    val m4 = FloatMatrix(Array(Array[Float](1, 0, 0), Array[Float](0, 1, 0), Array[Float](0, 0, 1)))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based int array equality") {
    val k1 = IntArrayKey("myArray")
    val m1 = IntArray(Array[Int](1, 2, 3))
    val m2 = IntArray(Array[Int](1, 2, 3))
    val m3 = IntArray(Array[Int](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based int matrix equality") {
    val k1 = IntMatrixKey("myMatrix")
    val m1 = IntMatrix(Array(Array[Int](1, 2, 3), Array[Int](2, 3, 6), Array[Int](4, 6, 12)))
    val m2 = IntMatrix(Array(Array[Int](1, 2, 3), Array[Int](2, 3, 6), Array[Int](4, 6, 12)))
    val m3 = IntMatrix(Array(Array[Int](1, 2, 3), Array[Int](2, 3, 6), Array[Int](0, 6, 12))) // Note one value different
    val m4 = IntMatrix(Array(Array[Int](1, 0, 0), Array[Int](0, 1, 0), Array[Int](0, 0, 1)))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

  describe("Array-based short array equality") {
    val k1 = ShortArrayKey("myArray")
    val m1 = ShortArray(Array[Short](1, 2, 3))
    val m2 = ShortArray(Array[Short](1, 2, 3))
    val m3 = ShortArray(Array[Short](1, 2, 4))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)

    it("should short circuit with identical same arrays") {
      assert(k1.set(m1).equals(k1.set(m1)))
    }

    it("should be equal with identical different arrays") {
      assert(k1.set(m1).equals(k1.set(m2)))
    }

    it("should fail with different valued arrays") {
      assert(!k1.set(m1).equals(k1.set(m3)))
    }

    it("should work with items too when equal") {
      assert(i1.equals(i2))
    }

    it("should fail with items when values not equal") {
      assert(!i2.equals(i3))
    }
  }

  describe("Array-based short matrix equality") {
    val k1 = ShortMatrixKey("myMatrix")
    val m1 = ShortMatrix(Array(Array[Short](1, 2, 3), Array[Short](2, 3, 6), Array[Short](4, 6, 12)))
    val m2 = ShortMatrix(Array(Array[Short](1, 2, 3), Array[Short](2, 3, 6), Array[Short](4, 6, 12)))
    val m3 = ShortMatrix(Array(Array[Short](1, 2, 3), Array[Short](2, 3, 6), Array[Short](0, 6, 12))) // Note one value different
    val m4 = ShortMatrix(Array(Array[Short](1, 0, 0), Array[Short](0, 1, 0), Array[Short](0, 0, 1)))
    val i1 = k1.set(m1)
    val i2 = k1.set(m2)
    val i3 = k1.set(m3)
    val i4 = k1.set(m4)

    it("should short circuit with identical same matrices") {
      assert(i1.equals(i1))
    }

    it("should be equal with identical different matrices") {
      assert(i1.equals(i2))
    }

    it("should fail with different valued matrices") {
      assert(!i2.equals(i3))
    }

    it("should fail for totally different matrices") {
      assert(!i3.equals(i4))
    }
  }

}
