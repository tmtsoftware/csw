package csw.param.parameters

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param.UnitsOfMeasure
import spray.json.DefaultJsonProtocol._
import spray.json.{JsArray, JsObject, JsString, JsValue, JsonFormat}

import scala.collection.immutable.Vector

object GParam {

  /**
   * type of a function that reads JSON and returns a new GenericItem
   */
  type JsonReaderFunc = JsValue => GParam[_]

  // Used to register a JsonFormat instance to use to read and write JSON for a given GenericItem subclass
  private var jsonReaderMap = Map[String, JsonReaderFunc]()

  /**
   * Sets the JSON reader and writer for a GenericItem
   *
   * @param typeName   the tag name in JSON
   * @param jsonReader implements creating this object from JSON
   * @tparam T the (scala) type parameter of the GenericItem
   */
  def register[T](typeName: String, jsonReader: JsonReaderFunc): Unit = jsonReaderMap += (typeName -> jsonReader)

  /**
   * Lookup the JsonFormat for the given type name
   *
   * @param typeName the JSON key
   * @return the JsonFormat, if registered
   */
  def lookup(typeName: String): Option[JsonReaderFunc] = jsonReaderMap.get(typeName)
}

/**
 * The type of a value for an GKey
 *
 * @param typeName the name of the type S (for JSON serialization)
 * @param keyName  the name of the key
 * @param values    the value for the key
 * @param units    the units of the value
 */
case class GParam[S: JsonFormat](typeName: String, keyName: String, values: Vector[S], units: Units)
    extends Parameter[S] {

  /**
   * @return a JsValue representing this item
   */
  def toJson: JsValue = {
    val valueFormat = implicitly[JsonFormat[S]]
    val unitsFormat = jsonFormat1(Units.apply)
    JsObject(
      "keyName" -> JsString(keyName),
      "value"   -> JsArray(values.map(valueFormat.write)),
      "units"   -> unitsFormat.write(units)
    )
  }

  override def withUnits(unitsIn: Units): Parameter[S /*, S*/ ] = copy(units = unitsIn)
}

/**
 * A key of S values
 *
 * @param typeName the name of the type S (for JSON serialization)
 * @param nameIn   the name of the key
 */
case class GKey[S: JsonFormat](typeName: String, nameIn: String) extends Key[S, GParam[S]](nameIn) {

  override def set(v: Vector[S], units: Units = NoUnits): GParam[S] =
    GParam(typeName, keyName, v, units)

  override def set(v: S*): GParam[S /*, S*/ ] =
    GParam(typeName, keyName, v.toVector, UnitsOfMeasure.NoUnits)
}
