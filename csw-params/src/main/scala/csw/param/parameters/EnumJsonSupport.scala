package csw.param.parameters

import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}
import language.higherKinds

object EnumJsonSupport extends DefaultJsonProtocol {
  def format[E[x] <: EnumEntry, T](enum: Enum[E[_]]): JsonFormat[E[T]] = new JsonFormat[E[T]] {
    override def write(obj: E[T]): JsValue = JsString(obj.entryName)
    override def read(json: JsValue): E[T] = enum.withName(json.convertTo[String]).asInstanceOf[E[T]]
  }
}
