package csw.param.parameters.matrices

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala equivalent of a 2d array of Bytes
 */
case class ByteMatrix(data: Array[Array[Byte]]) {

  override def toString = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")

  /**
   * Gets the value at the given row and column
   */
  def apply(row: Int, col: Int) = data(row)(col)

  override def canEqual(other: Any) = other.isInstanceOf[ByteMatrix]

  override def equals(other: Any) = other match {
    case that: ByteMatrix =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepMatrixValueEquals(this.data, that.data)
    case _ => false
  }
}

case object ByteMatrix extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(ByteMatrix.apply)

  implicit def create(value: Array[Array[Byte]]): ByteMatrix = ByteMatrix(value)
}

/**
 * The type of a value for an ByteMatrixKey: One or more 2d arrays (implemented as ByteMatrix)
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class ByteMatrixParameter(keyName: String, values: Vector[ByteMatrix], units: Units)
    extends Parameter[ByteMatrix] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for ByteMatrix values
 *
 * @param nameIn the name of the key
 */
final case class ByteMatrixKey(nameIn: String) extends Key[ByteMatrix, ByteMatrixParameter](nameIn) {

  override def set(v: Vector[ByteMatrix], units: Units = NoUnits) = ByteMatrixParameter(keyName, v, units)

  override def set(v: ByteMatrix*) = ByteMatrixParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
