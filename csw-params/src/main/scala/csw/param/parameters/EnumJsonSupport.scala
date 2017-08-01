package csw.param.parameters

import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

object EnumJsonSupport extends DefaultJsonProtocol {
  def format[E <: EnumEntry](enum: Enum[E]): JsonFormat[E] = new JsonFormat[E] {
    override def write(obj: E): JsValue = JsString(obj.entryName)
    override def read(json: JsValue): E = enum.withName(json.convertTo[String])
  }
}
