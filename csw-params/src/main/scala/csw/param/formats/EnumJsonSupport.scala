package csw.param.formats

import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

import scala.language.higherKinds

trait EnumJsonSupport { self: DefaultJsonProtocol ⇒
  def enumFormat[E[x] <: EnumEntry, T](enum: Enum[E[_]]): JsonFormat[E[T]] = new JsonFormat[E[T]] {
    override def write(obj: E[T]): JsValue = JsString(obj.entryName)
    override def read(json: JsValue): E[T] = enum.withName(json.convertTo[String]).asInstanceOf[E[T]]
  }
}
