package csw.messages.params.formats

import enumeratum.{Enum, EnumEntry}
import play.api.libs.json._

trait EnumJsonSupport { self ⇒
  def enumFormat[T <: EnumEntry](enum: Enum[T]): Format[T] = new Format[T] {
    override def writes(obj: T): JsValue = JsString(obj.entryName)
    override def reads(json: JsValue): JsSuccess[T] = {
      JsSuccess(enum.withName(json.toString.filterNot(_ == '"')))
    }
  }
}

object EnumJsonSupport extends EnumJsonSupport
