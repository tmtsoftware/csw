package csw.param

import java.util.UUID

import spray.json.{JsString, JsValue, JsonFormat}

/**
 * Implementation of unique id for each running command (returned from a queue submit).
 */
object RunId {
  import csw.param.formats.JsonSupport._

  implicit val format: JsonFormat[RunId] = new JsonFormat[RunId] {
    override def write(obj: RunId): JsValue = JsString(obj.id)
    override def read(json: JsValue): RunId = RunId(json.convertTo[String])
  }

  def apply(): RunId  = new RunId(UUID.randomUUID().toString)
  def create(): RunId = new RunId(UUID.randomUUID().toString)

  def apply(uuid: UUID): RunId = new RunId(uuid.toString)
}

case class RunId(id: String)
