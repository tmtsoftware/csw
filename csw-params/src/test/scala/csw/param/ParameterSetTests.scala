package csw.param

import csw.param.Parameters._
import csw.param.parameters.arrays.{LongArray, LongArrayKey}
import csw.param.parameters.primitives.{DoubleKey, IntKey, StringKey}
import org.scalatest.FunSpec
import spray.json.DefaultJsonProtocol

/**
 * Created by gillies on 5/25/17.
 */
//noinspection TypeAnnotation
object ParameterSetTests {

  import DefaultJsonProtocol._

  case class MyData(i: Int, f: Float, d: Double, s: String)

  implicit val MyDataFormat = jsonFormat4(MyData.apply)
}

//noinspection ComparingUnrelatedTypes,ScalaUnusedSymbol,TypeAnnotation
class ParameterSetTests extends FunSpec {

  private val ck1 = "wfos.prog.cloudcover"

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

  describe("test Configurations3 Setup") {
    val encoder1 = IntKey("encoder1")
    val encoder2 = IntKey("encoder2")

    val obsId = "Obs001"

    val sc1 = Setup(obsId, ck1).madd(encoder1.set(22), encoder2.set(33).withUnits(UnitsOfMeasure.degrees))

    assert(sc1.info.obsId.obsId == obsId)
    assert(sc1.subsystem == Subsystem.WFOS)
    assert(sc1.prefixStr == ck1)
    println(s"configkey: ${sc1.prefix}")
    println(s"runId: + ${sc1.info.runId}")

  }

}
