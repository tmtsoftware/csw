package csw.param.parameters

import csw.param.UnitsOfMeasure
import csw.param.UnitsOfMeasure.{NoUnits, Units}
import spray.json.JsonFormat

import scala.annotation.varargs
import scala.reflect.ClassTag

case class Key[S: JsonFormat: ClassTag] private[parameters] (keyName: String, keyType: KeyType[S]) {

  def set(v: Array[S], units: Units = NoUnits): Parameter[S] = Parameter(keyName, keyType, v, units)

  /**
   * Sets the values for the key using a variable number of arguments
   *
   * @param xs one or more values
   * @return a parameter containing the key name, values (call withUnits() on the result to gset the units)
   */
  @varargs
  def set(xs: S*): Parameter[S] = Parameter(keyName, keyType, xs.toArray[S], NoUnits)

  /**
   * Sets the values for the key
   * This definition enables writing code like this (see [[csw.param.ParameterSetDsl]]):
   * {{{
   *   val setup = sc(
   *    prefix,
   *     key1 -> value1 withUnits UnitsOfMeasure.Deg,
   *     key2 -> value2  // with default units
   *   )
   * }}}
   *
   * @param v the value
   * @return a parameter containing the key name and one value (call withUnits() on the result to gset the units)
   */
  def ->(v: S*): Parameter[S] = set(v: _*)

  /**
   * Sets the value and units for the key
   * This definition enables writing code like this (see [[csw.param.ParameterSetDsl]]):
   * {{{
   *   val setup = sc(
   *    prefix,
   *     key1 -> (value1, units1),
   *     key2 -> (value2, units2)
   *   )
   * }}}
   *
   * @param v a pair containing a single value for the key and the units of the value
   * @return a parameter containing the key name, values and units
   */
  def ->(v: (S, UnitsOfMeasure.Units)): Parameter[S] = set(Array(v._1), v._2)

  /**
   * Sets the values for the key as a Scala Vector
   * This definition enables writing code like this (see [[csw.param.ParameterSetDsl]]):
   * {{{
   *   val setup = sc(prefix,
   *     key1 -> Vector(...),
   *     key2 -> Vector(...)
   *   )
   * }}}
   *
   * @param v a vector of values
   * @return a parameter containing the key name and values (call withUnits() on the result to gset the units)
   */
  def ->(v: Array[S]): Parameter[S] = set(v)

  override def toString: String = keyName

  override def equals(that: Any): Boolean = {
    that match {
      case that: Key[S] => this.keyName == that.keyName
      case _            => false
    }
  }

  override def hashCode: Int = 41 * keyName.hashCode
}
