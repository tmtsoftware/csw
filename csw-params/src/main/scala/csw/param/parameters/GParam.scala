package csw.param.parameters

import csw.param.UnitsOfMeasure.{NoUnits, Units}
import spray.json.{pimpAny, DefaultJsonProtocol, JsObject, JsValue, JsonFormat, RootJsonFormat}

import scala.collection.immutable.Vector
import scala.collection.mutable
import scala.reflect.ClassTag
import language.implicitConversions

object GParam extends DefaultJsonProtocol {

  private[parameters] def apply[S: JsonFormat: ClassTag](
      keyName: String,
      keyType: KeyType[S],
      items: mutable.WrappedArray[S],
      units: Units
  ): GParam[S] =
    new GParam(keyName, keyType, items, units)

  implicit def parameterFormat[T: JsonFormat: ClassTag]: RootJsonFormat[GParam[T]] = new RootJsonFormat[GParam[T]] {
    override def write(obj: GParam[T]): JsValue = {
      JsObject(
        "keyName" -> obj.keyName.toJson,
        "keyType" -> obj.keyType.toJson,
        "items"   -> obj.items.array.toJson,
        "units"   -> obj.units.toJson
      )
    }

    override def read(json: JsValue): GParam[T] = {
      val fields = json.asJsObject.fields
      GParam(
        fields("keyName").convertTo[String],
        fields("keyType").convertTo[KeyType[T]],
        fields("items").convertTo[Array[T]],
        fields("units").convertTo[Units]
      )
    }
  }

  def apply[T](implicit x: JsonFormat[GParam[T]]): JsonFormat[GParam[T]] = x
}

case class GParam[S] private[param] (keyName: String,
                                     keyType: KeyType[S],
                                     items: mutable.WrappedArray[S],
                                     units: Units)(
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

class GChoiceKey(name: String, keyType: KeyType[Choice], val choices: Choices) extends GKey[Choice](name, keyType) {
  private def validate(xs: Seq[Choice]) =
    assert(xs.forall(choices.contains), s"Bad choice for key: $keyName which must be one of: $choices")

  override def set(v: Vector[Choice], units: Units): GParam[Choice] = {
    validate(v)
    super.set(v, units)
  }

  override def set(xs: Choice*): GParam[Choice] = {
    validate(xs)
    super.set(xs: _*)
  }

  override def gset(v: Array[Choice], units: Units): GParam[Choice] = {
    validate(v)
    super.gset(v, units)
  }
}

case class GKey[S] private[parameters] (name: String, keyType: KeyType[S])(implicit @transient jsFormat: JsonFormat[S],
                                                                           @transient clsTag: ClassTag[S])
    extends Key[S, GParam[S]](name) {

  override def set(v: Vector[S], units: Units = NoUnits): GParam[S] = GParam(name, keyType, v.toArray[S], units)

  override def set(xs: S*): GParam[S] = GParam(name, keyType, xs.toArray[S], NoUnits)
}

case class GArray[T](data: mutable.WrappedArray[T])

object GArray extends WrappedArrayProtocol with DefaultJsonProtocol {
  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[GArray[T]] =
    jsonFormat1((xs: mutable.WrappedArray[T]) ⇒ new GArray[T](xs))

  implicit def fromArray[T](xs: Array[T]): GArray[T] = new GArray(xs)

  def fromArray[T: ClassTag](xs: T*): GArray[T] = new GArray(xs.toArray[T])
}

case class GMatrix[T](data: mutable.WrappedArray[mutable.WrappedArray[T]]) {
  def apply(row: Int, col: Int): T = data(row)(col)
}

object GMatrix extends WrappedArrayProtocol with DefaultJsonProtocol {
  implicit def format[T: JsonFormat: ClassTag]: JsonFormat[GMatrix[T]] =
    jsonFormat1((xs: mutable.WrappedArray[mutable.WrappedArray[T]]) => new GMatrix[T](xs))

  implicit def fromArrays[T](xs: Array[Array[T]]): GMatrix[T] = new GMatrix[T](xs.map(x ⇒ x: mutable.WrappedArray[T]))

  def fromArrays[T: ClassTag](xs: Array[T]*): GMatrix[T] =
    new GMatrix[T](xs.toArray.map(x ⇒ x: mutable.WrappedArray[T]))
}

trait WrappedArrayProtocol { self: DefaultJsonProtocol ⇒
  implicit def wrappedArrayFormat[T: JsonFormat: ClassTag]: JsonFormat[mutable.WrappedArray[T]] =
    new JsonFormat[mutable.WrappedArray[T]] {
      override def write(obj: mutable.WrappedArray[T]): JsValue = obj.array.toJson
      override def read(json: JsValue): mutable.WrappedArray[T] = json.convertTo[Array[T]]
    }
}
