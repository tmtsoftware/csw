package csw.param.parameters.arrays

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala Vector of Longs
 */
case class LongArray(data: Array[Long]) {

  override def toString = data.mkString("X(", ",", ")")

  /**
   * Gets the value at the given index
   */
  def apply(idx: Int) = data(idx)

  override def canEqual(other: Any) = other.isInstanceOf[LongArray]

  override def equals(other: Any) = other match {
    case that: LongArray =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepArrayEquals(this.data, that.data)
    case _ => false
  }
}

case object LongArray extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(LongArray.apply)

  implicit def create(value: Array[Long]): LongArray = LongArray(value)
}

/**
 * The type of a value for a LongArrayKey: One or more arrays of Long
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class LongArrayParameter(keyName: String, values: Vector[LongArray], units: Units)
    extends Parameter[LongArray] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for LongArray values
 *
 * @param nameIn the name of the key
 */
final case class LongArrayKey(nameIn: String) extends Key[LongArray, LongArrayParameter](nameIn) {

  override def set(v: Vector[LongArray], units: Units = NoUnits) = LongArrayParameter(keyName, v, units)

  override def set(v: LongArray*) = LongArrayParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
