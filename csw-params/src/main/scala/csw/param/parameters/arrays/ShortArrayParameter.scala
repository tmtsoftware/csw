package csw.param.parameters.arrays

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala Array of Shorts
 */
case class ShortArray(data: Array[Short]) {

  override def toString = data.mkString("(", ",", ")")

  def apply(idx: Int) = data(idx)

  override def canEqual(other: Any) = other.isInstanceOf[ShortArray]

  override def equals(other: Any) = other match {
    case that: ShortArray =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepArrayEquals(this.data, that.data)
    case _ => false
  }
}
case object ShortArray extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(ShortArray.apply)

  implicit def create(value: Array[Short]): ShortArray = ShortArray(value)
}

/**
 * The type of a value for a ShortArrayKey: One or more arrays of Short
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class ShortArrayParameter(keyName: String, values: Vector[ShortArray], units: Units)
    extends Parameter[ShortArray] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for ShortArray values
 *
 * @param nameIn the name of the key
 */
final case class ShortArrayKey(nameIn: String) extends Key[ShortArray, ShortArrayParameter](nameIn) {

  override def set(v: Vector[ShortArray], units: Units = NoUnits) = ShortArrayParameter(keyName, v, units)

  override def set(v: ShortArray*) = ShortArrayParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
