package csw.param.parameters

import csw.param.ParameterSetJson._
import csw.param.UnitsOfMeasure.{NoUnits, Units}
import spray.json.{JsArray, JsObject, JsString, JsValue, JsonFormat, RootJsonFormat}

import scala.collection.immutable.Vector
import scala.reflect.ClassTag

object GParam {

  private[parameters] def apply[S](typeName: String,
                                   keyName: String,
                                   items: Vector[S],
                                   units: Units,
                                   valueFormat: JsonFormat[S]): GParam[S] =
    new GParam(typeName, keyName, items, units, valueFormat)

  implicit def parameterFormat[T: JsonFormat]: RootJsonFormat[GParam[T]] = new RootJsonFormat[GParam[T]] {
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
        fields("values").convertTo[Vector[T]],
        fields("units").convertTo[Units],
        implicitly[JsonFormat[T]]
      )
    }
  }

}

/**
 * The type of a value for an GKey
 *
 * @param typeName the name of the type S (for JSON serialization)
 * @param keyName  the name of the key
 * @param values    the value for the key
 * @param units    the units of the value
 */
case class GParam[S] private (typeName: String,
                              keyName: String,
                              values: Vector[S],
                              units: Units,
                              valueFormat: JsonFormat[S])
    extends Parameter[S] {

  /**
   * @return a JsValue representing this item
   */
  def toJson: JsValue                               = GParam.parameterFormat[S](valueFormat).write(this)
  override def withUnits(unitsIn: Units): GParam[S] = copy(units = unitsIn)
}

class GKey[S: JsonFormat: ClassTag] private[parameters] (nameIn: String, typeName: String)
    extends Key[S, GParam[S]](nameIn) {
  override def set(v: Vector[S], units: Units = NoUnits): GParam[S] =
    GParam(typeName, keyName, v, units, implicitly[JsonFormat[S]])
}
