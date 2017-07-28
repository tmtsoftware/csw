package csw.param.parameters.matrices

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import csw.param.parameters.{Key, Parameter}
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala equivalent of a 2d array of Longs
 */
case class LongMatrix(data: Array[Array[Long]]) {

  override def toString = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")

  /**
   * Gets the value at the given row and column
   */
  def apply(row: Int, col: Int) = data(row)(col)

  override def canEqual(other: Any) = other.isInstanceOf[LongMatrix]

  override def equals(other: Any) = other match {
    case that: LongMatrix =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepMatrixValueEquals(this.data, that.data)
    case _ => false
  }
}
case object LongMatrix extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(LongMatrix.apply)

  implicit def create(value: Array[Array[Long]]): LongMatrix = LongMatrix(value)
}

/**
 * The type of a value for an LongMatrixKey: One or more 2d arrays (implemented as LongMatrix)
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class LongMatrixParameter(keyName: String, values: Vector[LongMatrix], units: Units)
    extends Parameter[LongMatrix] {
  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for LongMatrix values
 *
 * @param nameIn the name of the key
 */
final case class LongMatrixKey(nameIn: String) extends Key[LongMatrix, LongMatrixParameter](nameIn) {

  override def set(v: Vector[LongMatrix], units: Units = NoUnits) = LongMatrixParameter(keyName, v, units)

  override def set(v: LongMatrix*) = LongMatrixParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
