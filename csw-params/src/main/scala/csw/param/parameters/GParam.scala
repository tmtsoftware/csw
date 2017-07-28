package csw.param.parameters

import csw.param.ParameterSetJson._
import csw.param.UnitsOfMeasure.{NoUnits, Units}
import spray.json.{JsValue, JsonFormat}

import scala.collection.immutable.Vector
import scala.reflect.ClassTag

object GParam {

  def apply[S](typeName: String,
               keyName: String,
               items: Vector[S],
               units: Units,
               valueFormat: JsonFormat[S]): GParam[S] =
    new GParam(typeName, keyName, items, units, valueFormat)
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
  def toJson: JsValue                       = parameterFormat[S](valueFormat).write(this)
  def fromJson(jsValue: JsValue): GParam[S] = parameterFormat[S](valueFormat).read(jsValue)

  override def withUnits(unitsIn: Units): GParam[S] = copy(units = unitsIn)
}

case class GKey[S: JsonFormat: ClassTag](nameIn: String, typeName: String) extends Key[S, GParam[S]](nameIn) {

  override def set(v: Vector[S], units: Units = NoUnits): GParam[S] =
    GParam(typeName, keyName, v, units, implicitly[JsonFormat[S]])
}
