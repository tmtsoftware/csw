package csw.param.parameters

import csw.param.UnitsOfMeasure.Units
import spray.json.{pimpAny, DefaultJsonProtocol, JsValue, JsonFormat}

import scala.collection.immutable.Vector
import scala.collection.mutable
import scala.language.implicitConversions
import scala.reflect.ClassTag

class GChoiceKey(name: String, keyType: KeyType[Choice], val choices: Choices) extends Key[Choice](name, keyType) {
  private def validate(xs: Seq[Choice]) =
    assert(xs.forall(choices.contains), s"Bad choice for key: $keyName which must be one of: $choices")

  override def set(v: Vector[Choice], units: Units): Parameter[Choice] = {
    validate(v)
    super.set(v, units)
  }

  override def set(xs: Choice*): Parameter[Choice] = {
    validate(xs)
    super.set(xs: _*)
  }

  override def gset(v: Array[Choice], units: Units): Parameter[Choice] = {
    validate(v)
    super.gset(v, units)
  }
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
