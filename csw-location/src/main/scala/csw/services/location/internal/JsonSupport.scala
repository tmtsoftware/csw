package csw.services.location.internal

import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

object JsonSupport extends DefaultJsonProtocol {
  def enumFormat[T <: EnumEntry](enum: Enum[T]): JsonFormat[T] = new JsonFormat[T] {
    override def write(obj: T): JsValue = JsString(obj.entryName)
    override def read(json: JsValue): T = enum.withName(json.convertTo[String])
  }
}
