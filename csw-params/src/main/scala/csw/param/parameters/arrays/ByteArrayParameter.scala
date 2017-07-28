package csw.param.parameters.arrays

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param._
import csw.param.parameters.{Key, Parameter}
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Vector
import scala.language.implicitConversions

/**
 * A Scala Vector of Bytes
 */
case class ByteArray(data: Array[Byte]) {

  override def toString = data.mkString("(", ",", ")")

  /**
   * Gets the value at the given index
   */
  def apply(idx: Int) = data(idx)

  override def canEqual(other: Any) = other.isInstanceOf[ByteArray]

  override def equals(other: Any) = other match {
    case that: ByteArray =>
      this.canEqual(that) && ArrayAndMatrixEquality.deepArrayEquals(this.data, that.data)
    case _ => false
  }
}

case object ByteArray extends DefaultJsonProtocol {
  implicit def format = jsonFormat1(ByteArray.apply)

  implicit def create(value: Array[Byte]): ByteArray = ByteArray(value)
}

/**
 * The type of a value for a ByteArrayKey: One or more arrays of Byte
 *
 * @param keyName the name of the key
 * @param values  the value for the key
 * @param units   the units of the value
 */
final case class ByteArrayParameter(keyName: String, values: Vector[ByteArray], units: Units)
    extends Parameter[ByteArray] {

  override def withUnits(unitsIn: Units) = copy(units = unitsIn)
}

/**
 * A key for ByteArray values
 *
 * @param nameIn the name of the key
 */
final case class ByteArrayKey(nameIn: String) extends Key[ByteArray, ByteArrayParameter](nameIn) {

  override def set(v: Vector[ByteArray], units: Units = NoUnits) = ByteArrayParameter(keyName, v, units)

  override def set(v: ByteArray*) = ByteArrayParameter(keyName, v.toVector, units = UnitsOfMeasure.NoUnits)
}
