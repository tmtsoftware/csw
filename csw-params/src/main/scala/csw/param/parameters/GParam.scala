package csw.param.parameters

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import spray.json.{pimpAny, DefaultJsonProtocol, JsObject, JsValue, JsonFormat, RootJsonFormat}

import scala.collection.immutable.Vector
import scala.collection.mutable
import scala.reflect.ClassTag

object GParam extends DefaultJsonProtocol {

  private[parameters] def apply[S: JsonFormat: ClassTag](typeName: String,
                                                         keyName: String,
                                                         items: mutable.WrappedArray[S],
                                                         units: Units): GParam[S] =
    new GParam(typeName, keyName, items, units)

  implicit def parameterFormat[T: JsonFormat: ClassTag]: RootJsonFormat[GParam[T]] = new RootJsonFormat[GParam[T]] {
    override def write(obj: GParam[T]): JsValue = {
      JsObject(
        "typeName" -> obj.typeName.toJson,
        "keyName"  -> obj.keyName.toJson,
        "items"    -> obj.items.array.toJson,
        "units"    -> obj.units.toJson
      )
    }

    override def read(json: JsValue): GParam[T] = {
      val fields = json.asJsObject.fields
      GParam(
        fields("typeName").convertTo[String],
        fields("keyName").convertTo[String],
        fields("items").convertTo[Array[T]],
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
case class GParam[S: JsonFormat: ClassTag] private[param] (typeName: String,
                                                           keyName: String,
                                                           items: mutable.WrappedArray[S],
                                                           units: Units)
    extends Parameter[S] {

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

  override def set(v: Vector[S], units: Units = NoUnits): GParam[S] = GParam(typeName, keyName, v.toArray[S], units)
}
