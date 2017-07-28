package csw.param.parameters.matrices

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala equivalent of a 2d array of Floats
 */
case class FloatMatrix(data: Array[Array[Float]]) {

  override def toString = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")

  /**
   * Gets the value at the given row and column
   */
  def apply(row: Int, col: Int) = data(row)(col)

  override def canEqual(other: Any) = other.isInstanceOf[FloatMatrix]

  override def equals(other: Any) = other match {
    case that: FloatMatrix =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepMatrixValueEquals(this.data, that.data)
    case _ => false
  }
}

case object FloatMatrix extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(FloatMatrix.apply)

  implicit def create(value: Array[Array[Float]]): FloatMatrix = FloatMatrix(value)
}

/**
 * The type of a value for a FloatMatrixKey: One or more 2d arrays (implemented as FloatMatrix)
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class FloatMatrixParameter(keyName: String, values: Vector[FloatMatrix], units: Units)
    extends Parameter[FloatMatrix] {
  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for FloatMatrix values
 *
 * @param nameIn the name of the key
 */
final case class FloatMatrixKey(nameIn: String) extends Key[FloatMatrix, FloatMatrixParameter](nameIn) {

  override def set(v: Vector[FloatMatrix], units: Units = NoUnits) = FloatMatrixParameter(keyName, v, units)

  override def set(v: FloatMatrix*) = FloatMatrixParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
