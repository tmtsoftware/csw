package csw.param

import csw.param.commands.Setup
import csw.param.models.ArrayData
import csw.param.parameters.KeyType
import csw.units.UnitsOfMeasure
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

  val k1 = KeyType.IntKey.make("itest")
  val k2 = KeyType.DoubleKey.make("dtest")
  val k3 = KeyType.StringKey.make("stest")
  val k4 = KeyType.LongArrayKey.make("lartest")

  val i1  = k1.set(1, 2, 3).withUnits(UnitsOfMeasure.degrees)
  val i11 = k1.set(1, 2, 3).withUnits(UnitsOfMeasure.degrees) // This is here to see if it is checking equality or address
  val i2  = k2.set(1.0, 2.0, 3.0).withUnits(UnitsOfMeasure.meters)
  val i3  = k3.set("A", "B", "C")
  val i4  = k4.set(ArrayData(Array.fill[Long](100)(10)), ArrayData(Array.fill[Long](100)(100)))
  val i5  = k1.set(22) // This is not added for testing not present removal

  describe("test Configurations3 Setup") {
    val encoder1 = KeyType.IntKey.make("encoder1")
    val encoder2 = KeyType.IntKey.make("encoder2")

    val obsId = "Obs001"

    val sc1 = Setup(obsId, ck1).madd(encoder1.set(22), encoder2.set(33).withUnits(UnitsOfMeasure.degrees))

    println(sc1)
    assert(sc1.info.obsId.obsId == obsId)
    assert(sc1.subsystem == Subsystem.WFOS)
    assert(sc1.prefixStr == ck1)
    println(s"configkey: ${sc1.prefix}")
    println(s"runId: + ${sc1.info.runId}")

  }

}
