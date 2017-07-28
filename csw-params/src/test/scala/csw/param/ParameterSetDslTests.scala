package csw.param

import csw.param.UnitsOfMeasure.{degrees, NoUnits}
import org.scalatest.{FunSpec, Matchers}
import csw.param.ParameterSetDsl._
import csw.param.Parameters.{CommandInfo, Setup}
import csw.param.parameters.arrays.{LongArray, LongArrayKey}
import csw.param.parameters.matrices.{LongMatrix, LongMatrixKey}
import csw.param.parameters.primitives._

/**
 * Tests the config DSL
 */
class ParameterSetDslTests extends FunSpec with Matchers {
  private val s1: String = "encoder"
  private val s2: String = "filter"
  private val s3: String = "detectorTemp"
  //  private val ck: String = "wfos.blue.filter"
  private val ck1: String = "wfos.prog.cloudcover"
  private val ck2: String = "wfos.red.filter"
  //  private val ck3: String = "wfos.red.detector"
  private val commandInfo = CommandInfo(ObsId("Obs001"))

  describe("creating items") {
    import csw.param.ParameterSetDsl.{size => ssize}

    val k1           = IntKey(s1)
    val detectorTemp = DoubleKey(s3)

    it("should work to set single items") {
      val i1 = set(k1, 2)
      i1 shouldBe an[IntParameter]
      ssize(i1) should equal(1)
      units(i1) should be(NoUnits)
    }

    it("should work to set multiple items") {
      val i1 = set(k1, 1, 2, 3, 4, 5)
      ssize(i1) should equal(5)
    }

    it("should work with units too") {
      val i1 = set(detectorTemp, 100.0).withUnits(degrees)
      i1 shouldBe an[DoubleParameter]
      ssize(i1) shouldBe 1
      units(i1) should be(degrees)
    }
  }

  describe("checking simple values") {
    import csw.param.ParameterSetDsl.{value => svalue}
    val k1 = IntKey(s1)

    it("should have value access") {
      val i1 = set(k1, 1, 2, 3, 4, 5)
      i1.size should equal(5)

      values(i1) should equal(Vector(1, 2, 3, 4, 5))
      head(i1) should equal(1)
      svalue(i1, 0) should equal(1)
      svalue(i1, 1) should equal(2)
      svalue(i1, 2) should equal(3)
      svalue(i1, 3) should equal(4)
      svalue(i1, 4) should equal(5)

      intercept[IndexOutOfBoundsException] {
        svalue(i1, 5)
      }
    }
  }

  describe("work with an array type") {
    val k1 = LongArrayKey(s2)

    it("should allow setting") {
      val m1: Array[Long] = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      val m2: Array[Long] = Array(100, 1000, 10000)

      val i1 = k1.set(m1, m2)
      i1.size should be(2)
      head(i1) should equal(LongArray(m1))
      head(i1).data should equal(m1)
      values(i1) should equal(Vector(LongArray(m1), LongArray(m2)))
      values(i1)(0) should equal(LongArray(m1))
      values(i1)(1) should equal(LongArray(m2))
    }
  }

  describe("work with a matrix type") {
    val k1 = LongMatrixKey(s2)

    it("should allow setting") {
      val m1: Array[Array[Long]] = Array(Array(1, 2, 4), Array(2, 4, 8), Array(4, 8, 16))
      // Note that LongMatrix implicit doesn't work here?
      val i1 = set(k1, LongMatrix(m1))
      i1.size should be(1)
      head(i1) should equal(LongMatrix(m1))
      head(i1).data should equal(m1)
      values(i1) should equal(Vector(LongMatrix(m1)))
      values(i1)(0) should equal(LongMatrix(m1))
    }
  }

  describe("checking optional get values") {
    val k1 = IntKey(s1)

    it("should have value access") {
      val i1 = set(k1, 1, 2, 3, 4, 5)
      i1.size should equal(5)
      values(i1) should equal(Vector(1, 2, 3, 4, 5))

      get(i1, 0) should equal(Option(1))
      get(i1, 1) should equal(Option(2))
      // Out of range gives None
      get(i1, 9) should equal(None)
    }
  }

  describe("adding items to sc") {
    val k1 = IntKey(s1)
    val k2 = StringKey(s2)
    val k3 = DoubleKey(s3)

    it("should allow adding single items") {
      val sc1 = add(Setup(commandInfo, ck1), set(k1, 1000))
      sc1.size should be(1)
    }

    it("shoudl allow adding several at once") {
      val sc2 = madd(Setup(commandInfo, ck2), set(k1, 1000), set(k2, "1000"), set(k3, 1000.0))

      sc2.size should be(3)
      exists(sc2, k1) shouldBe true
      exists(sc2, k2) shouldBe true
      exists(sc2, k3) shouldBe true
    }
  }

  describe("accessing items in an sc") {
    val k1 = IntKey(s1)
    val k2 = StringKey(s2)
    val k3 = DoubleKey(s3)

    val i1 = set(k1, 1000)
    val i2 = set(k2, "1000")
    val i3 = set(k3, 1000.0)

    it("should allow accessing existing items") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)
      sc1.size should be(3)

      parameter(sc1, k1) should equal(i1)
      parameter(sc1, k2) should equal(i2)
      parameter(sc1, k3) should equal(i3)
    }

    it("should throw NoSuchElementException if not present") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)

      val k4 = FloatKey("not present")

      exists(sc1, k1) shouldBe true
      exists(sc1, k2) shouldBe true
      exists(sc1, k3) shouldBe true
      exists(sc1, k4) shouldBe false

      intercept[NoSuchElementException] {
        parameter(sc1, k4)
      }
    }
  }

  describe("accessing items in an sc as option") {
    val k1 = IntKey(s1)
    val k2 = StringKey(s2)
    val k3 = DoubleKey(s3)

    val i1 = set(k1, 1000)
    val i2 = set(k2, "1000")
    val i3 = set(k3, 1000.0)

    it("should allow accessing existing items") {
      var sc1 = Setup(commandInfo, ck2)
      sc1 = madd(sc1, i1, i2, i3)
      csize(sc1) should be(3)

      get(sc1, k1) should equal(Option(i1))
      get(sc1, k2) should equal(Option(i2))
      get(sc1, k3) should equal(Option(i3))
    }

    it("should be None if not present") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)

      val k4 = FloatKey("not present")
      get(sc1, k1) should equal(Option(i1))
      get(sc1, k2) should equal(Option(i2))
      get(sc1, k3) should equal(Option(i3))
      get(sc1, k4) should equal(None)
    }
  }

  describe("should allow option get") {
    val k1 = IntKey(s1)
    val k2 = StringKey(s2)
    val k3 = DoubleKey(s3)
    val k4 = StringKey("Not Present")

    val i1 = set(k1, 1000, 2000)
    val i2 = set(k2, "1000", "2000")
    val i3 = set(k3, 1000.0, 2000.0)

    it("should allow accessing existing items") {
      val sc1 = madd(Setup(commandInfo, ck2), i1, i2, i3)
      csize(sc1) should be(3)

      get(sc1, k1, 0) should be(Some(1000))
      get(sc1, k2, 1) should be(Some("2000"))
      // Out of range
      get(sc1, k3, 3) should be(None)
      // Non existent item
      get(sc1, k4, 0) should be(None)
    }
  }

  describe("removing items from a configuration by keyname") {
    val k1 = IntKey("itest")
    val k2 = DoubleKey("dtest")
    val k3 = StringKey("stest")
    val k4 = LongArrayKey("lartest")

    val i1 = set(k1, 1, 2, 3).withUnits(UnitsOfMeasure.degrees)
    val i2 = set(k2, 1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)
    val i3 = set(k3, "A", "B", "C")
    val i4 = set(k4, LongArray(Array.fill[Long](100)(10)), LongArray(Array.fill[Long](100)(100)))

    it("Should allow removing one at a time") {
      var sc1 = madd(Setup(commandInfo, ck1), i1, i2, i3, i4)
      csize(sc1) should be(4)
      get(sc1, k1).isDefined should be(true)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k1)
      csize(sc1) should be(3)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k2)
      csize(sc1) should be(2)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k3)
      csize(sc1) should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      // Should allow removing non-existent
      sc1 = remove(sc1, k3)
      csize(sc1) should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, k4)
      csize(sc1) should be(0)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(false)

      // Add allows re-adding
      sc1 = add(sc1, i4)
      csize(sc1) should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)
    }
  }

  describe("removing items from a configuration as items") {
    val k1 = IntKey("itest")
    val k2 = DoubleKey("dtest")
    val k3 = StringKey("stest")
    val k4 = LongArrayKey("lartest")

    val i1 = set(k1, 1, 2, 3).withUnits(UnitsOfMeasure.degrees)
    val i2 = set(k2, 1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)
    val i3 = set(k3, "A", "B", "C")
    val i4 = set(k4, LongArray(Array.fill[Long](100)(10)), LongArray(Array.fill[Long](100)(100)))

    it("Should allow removing one at a time") {
      var sc1 = madd(Setup(commandInfo, ck1), i1, i2, i3, i4)
      sc1.size should be(4)
      get(sc1, k1).isDefined should be(true)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i1)
      sc1.size should be(3)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(true)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i2)
      sc1.size should be(2)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(true)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i3)
      sc1.size should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      // Should allow removing non-existent
      sc1 = remove(sc1, i3)
      sc1.size should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)

      sc1 = remove(sc1, i4)
      sc1.size should be(0)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(false)

      // Add allows re-adding
      sc1 = add(sc1, i4)
      sc1.size should be(1)
      get(sc1, k1).isDefined should be(false)
      get(sc1, k2).isDefined should be(false)
      get(sc1, k3).isDefined should be(false)
      get(sc1, k4).isDefined should be(true)
    }
  }

  describe("sc tests") {
    val k1 = IntKey("itest")
    val k2 = DoubleKey("dtest")
    //    val k3 = StringKey("stest")
    //    val k4 = LongArrayKey("lartest")

    val i1 = set(k1, 1, 2, 3).withUnits(UnitsOfMeasure.degrees)
    val i2 = set(k2, 1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)

    it("should allow creation") {
      val sc1 = setup(commandInfo, ck2, i1, i2)
      csize(sc1) should be(2)
      exists(sc1, k1) shouldBe true
      exists(sc1, k2) shouldBe true

      val sc2 = setup(
        commandInfo,
        ck2,
        k1 -> 3 withUnits UnitsOfMeasure.degrees,
        k2 -> 44.3 withUnits UnitsOfMeasure.meters
      )
      csize(sc2) should be(2)
      exists(sc2, k1) shouldBe true
      exists(sc2, k2) shouldBe true
      units(parameter(sc2, k1)) shouldBe UnitsOfMeasure.degrees
      units(parameter(sc2, k2)) shouldBe UnitsOfMeasure.meters
    }
  }

  describe("config as template tests") {
    val zeroPoint = IntKey("zeroPoint")
    val filter    = StringKey("filter")
    val mode      = StringKey("mode")

    val i1 = set(mode, "Fast")    // default value
    val i2 = set(filter, "home")  // default value
    val i3 = set(zeroPoint, 1000) // Where home is

    it("should create overriding defaults") {
      val default: Setup = setup(commandInfo, ck2, i1, i2, i3)

      val sc1 = add(default, zeroPoint -> 2000)

      val intItem = sc1(zeroPoint)

      csize(sc1) shouldBe 3
      parameter(sc1, zeroPoint) should equal(intItem)
      head(parameter(sc1, zeroPoint)) should equal(2000)
      parameter(sc1, filter) should equal(i2)
      parameter(sc1, mode) should equal(i1)

      // Check that default has not changed
      parameter(default, zeroPoint) should equal(i3)
      parameter(default, filter) should equal(i2)
      parameter(default, mode) should equal(i1)
    }
  }
}
