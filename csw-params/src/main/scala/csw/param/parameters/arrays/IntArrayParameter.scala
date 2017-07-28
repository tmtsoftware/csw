package csw.param.parameters.arrays

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala Vector of Ints
 */
case class IntArray(data: Array[Int]) {

  override def toString = data.mkString("(", ",", ")")

  def apply(idx: Int) = data(idx)

  override def canEqual(other: Any) = other.isInstanceOf[IntArray]

  override def equals(other: Any) = other match {
    case that: IntArray =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepArrayEquals(this.data, that.data)
    case _ => false
  }
}

case object IntArray extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(IntArray.apply)

  implicit def create(value: Array[Int]): IntArray = IntArray(value)
}

/**
 * The type of a value for a IntArrayKey: One or more arrays of Int
 *
 * @param keyName the name of the key
 * @param values   the value for the key
 * @param units   the units of the value
 */
final case class IntArrayParameter(keyName: String, values: Vector[IntArray], units: Units)
    extends Parameter[IntArray] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for IntArray values
 *
 * @param nameIn the name of the key
 */
final case class IntArrayKey(nameIn: String) extends Key[IntArray, IntArrayParameter](nameIn) {

  override def set(v: Vector[IntArray], units: Units = NoUnits) = IntArrayParameter(keyName, v, units)

  override def set(v: IntArray*) = IntArrayParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
