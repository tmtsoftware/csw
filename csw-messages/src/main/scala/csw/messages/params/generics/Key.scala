package csw.messages.params.generics

import csw.messages.params.models.Units
import csw.messages.params.models.Units.NoUnits
import csw.messages.params.pb.ItemsFactory
import play.api.libs.json.Format

import scala.annotation.varargs
import scala.reflect.ClassTag
import scala.runtime.ScalaRunTime._

case class Key[S: Format: ClassTag: ItemsFactory] private[generics] (keyName: String,
                                                                     keyType: KeyType[S],
                                                                     units: Units) {

  def this(keyName: String, keyType: KeyType[S]) = this(keyName, keyType, NoUnits)

  def set(v: Array[S], units: Units = NoUnits): Parameter[S] = Parameter(keyName, keyType, v, units)

  /**
   * Sets the values for the key using a variable number of arguments
   *
   * @param xs one or more values
   * @return a parameter containing the key name, values (call withUnits() on the result to set the units)
   */
  @varargs
  def set(xs: S*): Parameter[S] = Parameter(keyName, keyType, xs.toArray[S], units)

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
   * @param v the value
   * @return a parameter containing the key name and one value (call withUnits() on the result to set the units)
   */
  def ->(v: S*): Parameter[S] = set(v: _*)

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
   * @param v a pair containing a single value for the key and the units of the value
   * @return a parameter containing the key name, values and units
   */
  def ->(v: (S, Units)): Parameter[S] = set(Array(v._1), v._2)

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
   * @param v a vector of values
   * @return a parameter containing the key name and values (call withUnits() on the result to set the units)
   */
  def ->(v: Array[S]): Parameter[S] = set(v)

  override def toString: String = keyName

  override def equals(that: Any): Boolean = {
    that match {
      case that: Key[S] => this.keyName == that.keyName
      case _            => false
    }
  }

  override def hashCode: Int = _hashCode(this)
}
