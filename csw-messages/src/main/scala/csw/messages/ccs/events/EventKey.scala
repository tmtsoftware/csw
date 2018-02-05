package csw.messages.ccs.events

import play.api.libs.json.{Json, OFormat}

case class EventKey(name: String) {
  override def toString: String = name
}

object EventKey {
  implicit val format: OFormat[EventKey] = Json.format[EventKey]
}
