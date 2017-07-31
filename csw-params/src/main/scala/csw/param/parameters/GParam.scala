package csw.param.parameters

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import spray.json.{pimpAny, DefaultJsonProtocol, JsObject, JsValue, JsonFormat, RootJsonFormat}

import scala.collection.immutable.Vector
import scala.collection.mutable
import scala.reflect.ClassTag
import language.implicitConversions

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
case class GParam[S] private[param] (typeName: String, keyName: String, items: mutable.WrappedArray[S], units: Units)(
    implicit @transient jsFormat: JsonFormat[S],
    @transient cTag: ClassTag[S]
) extends Parameter[S] {

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

  override def set(xs: S*): GParam[S] = GParam(typeName, keyName, xs.toArray[S], NoUnits)
}

case class GArray[T](data: mutable.WrappedArray[T])

object GArray extends WrappedArrayProtocol with DefaultJsonProtocol {
  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[GArray[T]] = jsonFormat1(GArray[T])
  implicit def fromArray[T](xs: Array[T]): GArray[T]                  = GArray(xs)
}

//case class GMatrix[T](data: mutable.WrappedArray[mutable.WrappedArray[T]])
//
//object GMatrix extends WrappedArrayProtocol with DefaultJsonProtocol {
//  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[GMatrix[T]] = jsonFormat1(GMatrix[T])
//  implicit def fromArrays[T](xs: Array[Array[T]]): GMatrix[T]          = GMatrix[T](mutable.WrappedArray.make(xs))
//}

trait WrappedArrayProtocol { self: DefaultJsonProtocol ⇒
  implicit def wrappedArrayFormat[T: JsonFormat: ClassTag]: JsonFormat[mutable.WrappedArray[T]] =
    new JsonFormat[mutable.WrappedArray[T]] {
      override def write(obj: mutable.WrappedArray[T]): JsValue = obj.array.toJson
      override def read(json: JsValue): mutable.WrappedArray[T] = json.convertTo[Array[T]]
    }
}
