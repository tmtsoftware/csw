package csw.messages.params.generics

import csw.messages.params.models.Units
import csw.messages.params.models.Units.NoUnits
import play.api.libs.json.Format

import scala.annotation.varargs
import scala.reflect.ClassTag
import scala.runtime.ScalaRunTime._

/**
 * A generic Key class. Never meant to be instantiated directly. [[csw.messages.params.generics.KeyType]] exposes
 * allowed types of Keys and make method to create instances of Key.
 *
 * @param keyName the name of the key
 * @param keyType reference to an object of type KeyType[S]
 * @param units applicable units
 */
case class Key[S: Format: ClassTag] private[generics] (keyName: String, keyType: KeyType[S], units: Units) {

  /**
   * An overloaded constructor to create Key with no units
   *
   * @param keyName the name of the key
   * @param keyType reference to an object of type KeyType[S]
   * @return an instance of Key[S]
   */
  def this(keyName: String, keyType: KeyType[S]) = this(keyName, keyType, NoUnits)

  /**
   * Set values against this key
   *
   * @param values an Array of values
   * @param units applicable units
   * @return an instance of Parameter[S] containing the key name, values (call withUnits() on the result to set the units)
   */
  def set(values: Array[S], units: Units = NoUnits): Parameter[S] = Parameter(keyName, keyType, values, units)

  /**
   * Sets the values for the key using a variable number of arguments
   *
   * @param values one or more values
   * @return an instance of Parameter[S] containing the key name, values (call withUnits() on the result to set the units)
   */
  @varargs
  def set(values: S*): Parameter[S] = Parameter(keyName, keyType, values.toArray[S], units)

  /**
   * Sets the values for the key
   * This definition enables writing code like this:
   * {{{
   *   val setup = sc(
   *    prefix,
   *     key1 -> value1 withUnits UnitsOfMeasure.Deg,
   *     key2 -> value2  // with default units
   *   )
   * }}}
   *
   * @param values one or more values
   * @return a parameter containing the key name and one value (call withUnits() on the result to set the units)
   */
  def ->(values: S*): Parameter[S] = set(values: _*)

  /**
   * Sets the value and units for the key
   * This definition enables writing code like this:
   * {{{
   *   val setup = sc(
   *    prefix,
   *     key1 -> (value1, units1),
   *     key2 -> (value2, units2)
   *   )
   * }}}
   *
   * @param values a pair containing a single value for the key and the units of the value
   * @return a parameter containing the key name, values and units
   */
  def ->(values: (S, Units)): Parameter[S] = set(Array(values._1), values._2)

  /**
   * Sets the values for the key as a Scala Vector
   * This definition enables writing code like this:
   * {{{
   *   val setup = sc(prefix,
   *     key1 -> Vector(...),
   *     key2 -> Vector(...)
   *   )
   * }}}
   *
   * @param values an Array of values
   * @return a parameter containing the key name and values (call withUnits() on the result to set the units)
   */
  def ->(values: Array[S]): Parameter[S] = set(values)

  /**
   * Returns a string representation of Key as keyName
   */
  override def toString: String = keyName

  /**
   * Equals this Key instance with other by the keyName
   *
   * @param that the other Key instance that is to be equated against this Key instance
   * @return a Boolean indicating whether two Key instances are equal or not
   */
  override def equals(that: Any): Boolean = {
    that match {
      case that: Key[S] => this.keyName == that.keyName
      case _            => false
    }
  }

  override def hashCode: Int = _hashCode(this)
}
