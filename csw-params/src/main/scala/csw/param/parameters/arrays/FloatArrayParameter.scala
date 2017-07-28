package csw.param.parameters.arrays

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import csw.param.parameters.{Key, Parameter}
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala Array of Floats
 */
case class FloatArray(data: Array[Float]) {

  override def toString = data.mkString("(", ",", ")")

  /**
   * Gets the value at the given index
   */
  def apply(idx: Int) = data(idx)

  override def canEqual(other: Any) = other.isInstanceOf[FloatArray]

  override def equals(other: Any) = other match {
    case that: FloatArray =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepArrayEquals(this.data, that.data)
    case _ => false
  }
}

case object FloatArray extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(FloatArray.apply)

  implicit def create(value: Array[Float]): FloatArray = FloatArray(value)
}

/**
 * The type of a value for a FloatVectorKey: One or more vectors of Float
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class FloatArrayParameter(keyName: String, values: Vector[FloatArray], units: Units)
    extends Parameter[FloatArray] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for FloatArray values
 *
 * @param nameIn the name of the key
 */
final case class FloatArrayKey(nameIn: String) extends Key[FloatArray, FloatArrayParameter](nameIn) {

  override def set(v: Vector[FloatArray], units: Units = NoUnits) = FloatArrayParameter(keyName, v, units)

  override def set(v: FloatArray*) = FloatArrayParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
