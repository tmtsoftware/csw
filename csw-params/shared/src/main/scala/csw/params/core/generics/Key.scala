package csw.params.core.generics

import csw.params.core.models.Units
import scala.annotation.varargs
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.runtime.ScalaRunTime._

/**
 * A generic Key class. Never meant to be instantiated directly. [[csw.params.core.generics.KeyType]] exposes
 * allowed types of Keys and make method to create instances of Key.
 *
 * @param keyName the name of the key
 * @param keyType reference to an object of type KeyType[S]
 * @param units applicable units
 */
case class Key[S: ClassTag] private[generics] (keyName: String, keyType: KeyType[S], units: Units) {
  private val invalidKeyName = List("[", "]", "/")
  require(!invalidKeyName.exists(keyName.contains), "Invalid Key name: Key cannot have '[' , ']' or '/' in its name")

  /**
   * Set values against this key
   *
   * @param values an Array of values
   * @return an instance of Parameter[S] containing the key name, values (call withUnits() on the result to set the units)
   */
  def setAll(values: Array[S]): Parameter[S] =
    Parameter(keyName, keyType, mutable.ArraySeq.make(values), units)

  /**
   * Sets the values for the key using a variable number of arguments
   *
   * @param values one or more values
   * @return an instance of Parameter[S] containing the key name, values (call withUnits() on the result to set the units)
   */
  @varargs
  def set(value: S, values: S*): Parameter[S] =
    Parameter(keyName, keyType, mutable.ArraySeq.make(value +: values.toArray[S]), units)

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
   * @return a parameter containing the key name and one value (call withUnits() on the result to set the units)
   */
  def ->(value: S): Parameter[S] = set(value)

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
