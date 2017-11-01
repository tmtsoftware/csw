package csw.messages.params.models

import java.util.UUID

import play.api.libs.json._

/**
 * Implementation of unique id for each running command (returned from a queue submit).
 */
case class RunId private (id: String)

object RunId {
  def apply(): RunId = new RunId(UUID.randomUUID().toString)

  implicit val format: Format[RunId] = new Format[RunId] {
    override def writes(obj: RunId): JsValue           = JsString(obj.id)
    override def reads(json: JsValue): JsResult[RunId] = JsSuccess(RunId(Json.stringify(json)))
  }
}
