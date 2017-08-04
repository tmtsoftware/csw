package csw.param

import csw.param.parameters.ParameterSetDsl._
import csw.param.commands.CommandInfo
import csw.param.models.{MatrixData, ObsId}
import csw.param.parameters.KeyType
import csw.param.parameters.KeyType.DoubleMatrixKey
import csw.units.Units.{degrees, meters}
import org.scalatest.FunSpec

/**
 * Test DSL for configs
 */
class ParameterSetDsl2Tests extends FunSpec {

  val commandInfo = CommandInfo(ObsId("Obs001"))

  describe("Tests DSL functions") {
    val k1 = KeyType.IntKey.make("itest")
    val k2 = KeyType.DoubleKey.make("dtest")
    val k3 = KeyType.StringKey.make("stest")
    val k4 = DoubleMatrixKey.make("myMatrix")

    val i1 = set(k1, 1, 2, 3).withUnits(degrees)
    val i2 = set(k2, 1.0, 2.0, 3.0).withUnits(meters)
    val i3 = set(k3, "A", "B", "C")

    it("should allow using head(item) and value(item)") {
      assert(head(i1) == 1)
      assert(value(i1, 1) == 2)
      assert(value(i1, 2) == 3)
      assert(get(i1, 3).isEmpty)
      assert(values(i1) === Array(1, 2, 3))
      assert(i1 == vset(k1, Array(1, 2, 3), degrees))

      assert(head(i2) == 1.0)
      assert(value(i2, 1) == 2.0)
      assert(value(i2, 2) == 3.0)
      assert(get(i2, 3).isEmpty)
      assert(values(i2) === Array(1.0, 2.0, 3.0))
      assert(i2 == vset(k2, Array(1.0, 2.0, 3.0), meters))

      assert(head(i3) == "A")
      assert(value(i3, 1) == "B")
      assert(value(i3, 2) == "C")
      assert(get(i3, 3).isEmpty)
      assert(values(i3) === Array("A", "B", "C"))
      assert(i3 == vset(k3, Array("A", "B", "C")))
    }

    it("should support key -> value syntax for building configs") {
      val dm1 = MatrixData.fromArrays(Array[Double](1, 2, 3), Array[Double](2, 3, 6), Array[Double](4, 6, 12))
      val setupConfig1 = setup(
        commandInfo,
        "test",
        k1 -> Array(1, 2, 3) withUnits degrees,
        k2 -> Array(1.0, 2.0, 3.0) withUnits meters,
        k3 -> Array("A", "B", "C"),
        k4 -> dm1 withUnits degrees
      )
      assert(setupConfig1.get(k1).get.values === Array(1, 2, 3))
      assert(setupConfig1.get(k1).get.units == degrees)
      assert(setupConfig1.get(k2).get.head == 1.0)
      assert(setupConfig1.get(k2).get.units == meters)
      assert(setupConfig1.get(k3).get.value(1) == "B")
      assert(setupConfig1.get(k4).get.head(0, 0) == 1)

      val setupConfig2 = setup(
        commandInfo,
        "test",
        k1 -> 1 withUnits degrees,
        k2 -> (2.0 → meters),
        k3 -> "C"
      )
      assert(get(setupConfig2, k1).get.head == 1)
      assert(get(setupConfig2, k1).get.units == degrees)
      assert(setupConfig2.get(k2).get.head == 2.0)
      assert(setupConfig2.get(k2).get.units == meters)
      assert(setupConfig2.get(k3).get.head == "C")
    }
  }
}
