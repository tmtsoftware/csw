package csw.param.parameters

import csw.param
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

/**
 * A key of S values
 *
 * @param nameIn   the name of the key
 */
sealed class GKey[S: JsonFormat: ClassTag](nameIn: String) extends Key[S, GParam[S]](nameIn) {

  val typeName: String = getClass.getSimpleName

  override def set(v: Vector[S], units: Units = NoUnits): GParam[S] =
    GParam(typeName, keyName, v, units, implicitly[JsonFormat[S]])
}

object Keys {
  case class RaDec(name: String)   extends GKey[csw.param.RaDec](name)
  case class Integer(name: String) extends GKey[scala.Int](name)
  case class Boolean(name: String) extends GKey[scala.Boolean](name)
}

object JKeys {
  case class Integer(name: String) extends GKey[java.lang.Integer](name)
  case class Boolean(name: String) extends GKey[java.lang.Boolean](name)
}

object Formats {
  val values: Map[String, JsonFormat[GParam[_]]] = Map(
    getPair[param.RaDec],
    getPair[java.lang.Integer],
    getPair[java.lang.Boolean],
  )

  print(values)

  def getPair[T: ClassTag: JsonFormat]: (String, JsonFormat[GParam[_]]) = (
    implicitly[ClassTag[T]].runtimeClass.getSimpleName,
    implicitly[JsonFormat[GParam[T]]].asInstanceOf[JsonFormat[GParam[_]]]
  )
}
