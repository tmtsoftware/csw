package csw.messages.params.formats

import enumeratum.{Enum, EnumEntry}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

trait EnumJsonSupport { self: DefaultJsonProtocol ⇒
  def enumFormat[T <: EnumEntry](enum: Enum[T]): JsonFormat[T] = new JsonFormat[T] {
    override def write(obj: T): JsValue = JsString(obj.entryName)
    override def read(json: JsValue): T = enum.withName(json.convertTo[String])
  }
}

object EnumJsonSupport extends EnumJsonSupport with DefaultJsonProtocol
