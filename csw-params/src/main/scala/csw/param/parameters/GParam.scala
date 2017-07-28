package csw.param.parameters

import csw.param.JsonSupport._
import csw.param.UnitsOfMeasure.{NoUnits, Units}
import spray.json.{JsArray, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat}

import scala.collection.immutable.Vector
import scala.reflect.ClassTag

object GParam {

  private[parameters] def apply[S: JsonFormat: ClassTag](typeName: String,
                                                         keyName: String,
                                                         items: Array[S],
                                                         units: Units): GParam[S] =
    new GParam(typeName, keyName, items, units)

  implicit def parameterFormat[T: JsonFormat: ClassTag]: RootJsonFormat[GParam[T]] = new RootJsonFormat[GParam[T]] {
    override def write(obj: GParam[T]): JsValue = {
      JsObject(
        "typeName" -> JsString(obj.typeName),
        "keyName"  -> JsString(obj.keyName),
        "values"   -> JsArray(obj.values.map(implicitly[JsonFormat[T]].write)),
        "units"    -> Units.unitsFormat.write(obj.units)
      )
    }

    override def read(json: JsValue): GParam[T] = {
      val fields = json.asJsObject.fields
      GParam(
        fields("typeName").convertTo[String],
        fields("keyName").convertTo[String],
        fields("values").convertTo[Vector[T]].toArray,
        fields("units").convertTo[Units]
      )
    }
  }

  def apply[T](implicit x: JsonFormat[GParam[T]]): JsonFormat[GParam[T]] = x
}

/**
 * The type of a value for an GKey
 *
 * @param typeName the name of the type S (for JSON serialization)
 * @param keyName  the name of the key
 * @param items    the value for the key
 * @param units    the units of the value
 */
case class GParam[S: JsonFormat: ClassTag] private (typeName: String, keyName: String, items: Array[S], units: Units)
    extends Parameter[S]
    with Proxy {

  override def self: Any = (typeName: String, keyName: String, items.toVector, units)

  /**
   * @return All the values for this parameter
   */
  override def values: Vector[S] = items.toVector

  /**
   * @return a JsValue representing this item
   */
  def toJson: JsValue                               = GParam.parameterFormat[S].write(this)
  override def withUnits(unitsIn: Units): GParam[S] = copy(units = unitsIn)
}

class GKey[S: JsonFormat: ClassTag] private[parameters] (nameIn: String, typeName: String)
    extends Key[S, GParam[S]](nameIn) {

  override def set(v: Vector[S], units: Units = NoUnits): GParam[S] = GParam(typeName, keyName, v.toArray, units)
}
