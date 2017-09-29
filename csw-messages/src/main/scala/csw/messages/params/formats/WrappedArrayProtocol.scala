package csw.messages.params.formats

import spray.json.{pimpAny, DefaultJsonProtocol, JsValue, JsonFormat}

import scala.collection.mutable
import scala.reflect.ClassTag

trait WrappedArrayProtocol { self: DefaultJsonProtocol ⇒
  implicit def wrappedArrayFormat[T: JsonFormat: ClassTag]: JsonFormat[mutable.WrappedArray[T]] =
    new JsonFormat[mutable.WrappedArray[T]] {
      override def write(obj: mutable.WrappedArray[T]): JsValue = obj.array.toJson
      override def read(json: JsValue): mutable.WrappedArray[T] = json.convertTo[Array[T]]
    }
}
