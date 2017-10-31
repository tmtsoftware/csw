package csw.messages.params.models

import java.util.UUID

import spray.json.{JsString, JsValue, JsonFormat}

/**
 * Implementation of unique id for each running command (returned from a queue submit).
 */
case class RunId private (id: String)

object RunId {
  def apply(): RunId = new RunId(UUID.randomUUID().toString)

  import spray.json.DefaultJsonProtocol._
  implicit val format: JsonFormat[RunId] = new JsonFormat[RunId] {
    override def write(obj: RunId): JsValue = JsString(obj.id)
    override def read(json: JsValue): RunId = RunId(json.convertTo[String])
  }
}
