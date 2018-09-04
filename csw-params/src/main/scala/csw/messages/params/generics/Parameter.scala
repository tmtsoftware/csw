package csw.messages.params.generics

import java.util

import csw.messages.TMTSerializable
import csw.messages.params.models.Units
import play.api.libs.json._

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable
import scala.reflect.ClassTag

object Parameter {

  private[params] def apply[S: Format: ClassTag](
      keyName: String,
      keyType: KeyType[S],
      items: mutable.WrappedArray[S],
      units: Units
  ): Parameter[S] =
    new Parameter(keyName, keyType, items, units)

  private[messages] implicit def parameterFormat2: Format[Parameter[_]] = new Format[Parameter[_]] {
    override def writes(obj: Parameter[_]): JsValue = obj.toJson

    override def reads(json: JsValue): JsResult[Parameter[_]] = {
      val value = (json \ "keyType").as[KeyType[_]]
      value.paramFormat.reads(json)
    }
  }

  private[messages] implicit def parameterFormat[T: Format: ClassTag]: Format[Parameter[T]] =
    new Format[Parameter[T]] {
      override def writes(obj: Parameter[T]): JsValue = {
        JsObject(
          Seq(
            "keyName" -> JsString(obj.keyName),
            "keyType" -> JsString(obj.keyType.entryName),
            "values"  -> Json.toJson(obj.values),
            "units"   -> JsString(obj.units.entryName)
          )
        )
      }

      override def reads(json: JsValue): JsResult[Parameter[T]] = {
        JsSuccess(
          Parameter(
            (json \ "keyName").as[String],
            (json \ "keyType").as[KeyType[T]],
            (json \ "values").as[Array[T]],
            (json \ "units").as[Units]
          )
        )
      }
    }

  def apply[T](implicit x: Format[Parameter[T]]): Format[Parameter[T]] = x
}

/**
 * Parameter represents a KeyName, KeyType, array of values and units applicable to values. Parameter sits as payload for
 * sending commands and events between components.
 *
 * @param keyName the name of the key
 * @param keyType reference to an object of type KeyType[S]
 * @param items an Array of values of type S
 * @param units applicable units
 * @tparam S the type of items this parameter holds
 */
case class Parameter[S: Format: ClassTag] private[messages] (
    keyName: String,
    keyType: KeyType[S],
    items: mutable.WrappedArray[S],
    units: Units
) extends TMTSerializable {

  /**
   * An Array of values this parameter holds
   */
  def values: Array[S] = items.array

  /**
   * A Java helper that returns a List of values this parameter holds
   */
  def jValues: util.List[S] = items.asJava

  /**
   * The number of values in this parameter (values.size)
   *
   * @return length of the array of items
   */
  def size: Int = items.size

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
  def value(index: Int): S = items(index)

  /**
   * Get method returns an option of value if present at the given index else none
   *
   * @param index the index of a value
   * @return some value at the given index as an Option, if the index is in range, otherwise None
   */
  def get(index: Int): Option[S] = items.lift(index)

  /**
   * Returns the first value as a convenience when storing a single value
   *
   * @return the first or default value (Use this if you know there is only a single value)
   */
  def head: S = value(0)

  /**
   * Sets the units for the values
   *
   * @param unitsIn the units for the values
   * @return a new instance of this parameter with the units set
   */
  def withUnits(unitsIn: Units): Parameter[S] = copy(units = unitsIn)

  /**
   * A comma separated string representation of all values this parameter holds
   */
  def valuesToString: String = items.mkString("(", ",", ")")

  /**
   * Returns a formatted string representation with a KeyName
   */
  override def toString: String = s"$keyName($valuesToString$units)"

  /**
   * Returns a JSON representation of this parameter
   */
  def toJson: JsValue = Parameter[S].writes(this)
}
