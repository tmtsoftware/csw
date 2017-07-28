package csw.param.parameters.matrices

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala equivalent of a 2d array of Shorts
 */
case class ShortMatrix(data: Array[Array[Short]]) {

  override def toString = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")

  /**
   * Gets the value at the given row and column
   */
  def apply(row: Int, col: Int) = data(row)(col)

  override def canEqual(other: Any) = other.isInstanceOf[ShortMatrix]

  override def equals(other: Any) = other match {
    case that: ShortMatrix =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepMatrixValueEquals(this.data, that.data)
    case _ => false
  }
}

case object ShortMatrix extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(ShortMatrix.apply)

  implicit def create(value: Array[Array[Short]]): ShortMatrix = ShortMatrix(value)
}

/**
 * A key for ShortMatrix values
 *
 * @param nameIn the name of the key
 */
final case class ShortMatrixKey(nameIn: String) extends Key[ShortMatrix, ShortMatrixParameter](nameIn) {

  override def set(v: Vector[ShortMatrix], units: Units = NoUnits) = ShortMatrixParameter(keyName, v, units)

  override def set(v: ShortMatrix*) = ShortMatrixParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}

/**
 * The type of a value for an ShortMatrixKey: One or more 2d arrays (implemented as ShortMatrix)
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class ShortMatrixParameter(keyName: String, values: Vector[ShortMatrix], units: Units)
    extends Parameter[ShortMatrix] {
  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}
