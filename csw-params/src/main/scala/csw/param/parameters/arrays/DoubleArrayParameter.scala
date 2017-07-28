package csw.param.parameters.arrays

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import csw.param.parameters.{Key, Parameter}
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala Array of Doubles
 */
case class DoubleArray(data: Array[Double]) {

  override def toString = data.mkString("(", ",", ")")

  /**
   * Gets the value at the given index
   */
  def apply(idx: Int) = data(idx)

  override def canEqual(other: Any) = other.isInstanceOf[DoubleArray]

  override def equals(other: Any) = other match {
    case that: DoubleArray =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepArrayEquals(this.data, that.data)
    case _ => false
  }
}

case object DoubleArray extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(DoubleArray.apply)

  implicit def create(value: Array[Double]): DoubleArray = DoubleArray(value)
}

/**
 * The type of a value for a DoubleVectorKey: One or more vectors of Double
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class DoubleArrayParameter(keyName: String, values: Vector[DoubleArray], units: Units)
    extends Parameter[DoubleArray] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for DoubleArray values
 *
 * @param nameIn the name of the key
 */
final case class DoubleArrayKey(nameIn: String) extends Key[DoubleArray, DoubleArrayParameter](nameIn) {

  override def set(v: Vector[DoubleArray], units: Units = NoUnits) = DoubleArrayParameter(keyName, v, units)

  override def set(v: DoubleArray*) = DoubleArrayParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
