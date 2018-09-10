package csw.messages.events

import play.api.libs.json._

/**
 * A wrapper class representing the name of an Event
 */
case class EventName(name: String) {
  override def toString: String = name
}

object EventName {
  implicit val format: Format[EventName] = new Format[EventName] {
    override def writes(obj: EventName): JsValue           = JsString(obj.name)
    override def reads(json: JsValue): JsResult[EventName] = JsSuccess(EventName(json.as[String]))
  }
}
