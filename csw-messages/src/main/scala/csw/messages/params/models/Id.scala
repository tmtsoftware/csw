package csw.messages.params.models

import java.util.UUID

import play.api.libs.json._

/**
 * Implementation of unique id fulfilling TMT requirements (returned from a queue submit).
 */
case class Id(id: String)

object Id {
  def apply(): Id = new Id(UUID.randomUUID().toString)

  implicit val format: Format[Id] = new Format[Id] {
    override def writes(obj: Id): JsValue           = JsString(obj.id)
    override def reads(json: JsValue): JsResult[Id] = JsSuccess(Id(json.as[String]))
  }
}
