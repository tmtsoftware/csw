package csw.param.parameters

import csw.param.UnitsOfMeasure
import csw.param.UnitsOfMeasure.{NoUnits, Units}

import scala.annotation.varargs
import scala.collection.immutable.Vector

/**
 * The type of a parameter in a parameterSet
 *
 * @tparam S the Scala type
 */
trait Parameter[S] {

  /**
   * @return the name of the key for this parameter
   */
  def keyName: String

  /**
   * @return the units for the values
   */
  def units: Units

  /**
   * @return All the values for this parameter
   */
  def values: Vector[S]

  /**
   * The number of values in this parameter (values.size)
   *
   * @return
   */
  def size: Int = values.size

  /**
   * Returns the value at the given index, throwing an exception if the index is out of range
   *
   * @param index the index of a value
   * @return the value at the given index (may throw an exception if the index is out of range)
   */
  def apply(index: Int): S = value(index)

  /**
   * Returns the value at the given index, throwing an exception if the index is out of range
   * This is a Scala convenience method
   *
   * @param index the index of a value
   * @return the value at the given index (may throw an exception if the index is out of range)
   */
  def value(index: Int): S = values(index)

  /**
   * @param index the index of a value
   * @return Some value at the given index as an Option, if the index is in range, otherwise None
   */
  def get(index: Int): Option[S] = values.lift(index)

  /**
   * Returns the first value as a convenience when storing a single value
   *
   * @return the first or default value (Use this if you know there is only a single value)
   */
  def head: S = value(0)

  /**
   * Sets the units for the values
   *
   * @param units the units for the values
   * @return a new instance of this parameter with the units gset
   */
  def withUnits(units: Units): Parameter[S]

  def valuesToString: String = values.mkString("(", ",", ")")
  override def toString      = s"$keyName($valuesToString$units)"
}

/**
 * The type of a parameter key.
 * Note that the Parameter is f-bounded polymorphic so that parameter returns will have the correct types
 *
 * @param keyName the key
 * @tparam S the value's Scala type
 * @tparam I the type of the parameter created by this Key
 */
abstract class Key[S, I <: Parameter[S]](val keyName: String) extends Serializable {

  def gset(v: Array[S], units: Units = NoUnits): I = set(v.toVector, units)

  /**
   * Sets the values for the key as a Scala Vector
   *
   * @param v     a vector of values
   * @param units optional units of the values (defaults to no units)
   * @return a parameter containing the key name, values and units
   */
  def set(v: Vector[S], units: Units = NoUnits): I

  /**
   * Sets the values for the key using a variable number of arguments
   *
   * @param v one or more values
   * @return a parameter containing the key name, values (call withUnits() on the result to gset the units)
   */
  @varargs
  def set(v: S*): I = set(v.toVector)

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
  def ->(v: S): I = set(v)

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
  def ->(v: (S, UnitsOfMeasure.Units)): I = set(Vector(v._1), v._2)

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
  def ->(v: Vector[S]): I = set(v)

  override def toString: String = keyName

  override def equals(that: Any): Boolean = {
    that match {
      case that: Key[S, I] => this.keyName == that.keyName
      case _               => false
    }
  }

  override def hashCode: Int = 41 * keyName.hashCode
}
