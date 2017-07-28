package csw.param.parameters.matrices

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala equivalent of a 2d array of Ints
 */
case class IntMatrix(data: Array[Array[Int]]) {

  override def toString = (for (l <- data) yield l.mkString("(", ",", ")")).mkString("(", ",", ")")

  def apply(row: Int, col: Int) = data(row)(col)

  override def canEqual(other: Any) = other.isInstanceOf[IntMatrix]

  override def equals(other: Any) = other match {
    case that: IntMatrix =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepMatrixValueEquals(this.data, that.data)
    case _ => false
  }
}
case object IntMatrix extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(IntMatrix.apply)

  implicit def create(value: Array[Array[Int]]): IntMatrix = IntMatrix(value)
}

/**
 * The type of a value for an IntMatrixKey: One or more 2d arrays (implemented as IntMatrix)
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class IntMatrixParameter(keyName: String, values: Vector[IntMatrix], units: Units)
    extends Parameter[IntMatrix] {
  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for IntMatrix values
 *
 * @param nameIn the name of the key
 */
final case class IntMatrixKey(nameIn: String) extends Key[IntMatrix, IntMatrixParameter](nameIn) {

  override def set(v: Vector[IntMatrix], units: Units = NoUnits) = IntMatrixParameter(keyName, v, units)

  override def set(v: IntMatrix*) = IntMatrixParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
