package csw.messages.ccs.events

import play.api.libs.json.{Json, OFormat}

/**
 * Model representing the name of an Event
 */
case class EventName(name: String)

object EventName {
  implicit val format: OFormat[EventName] = Json.format[EventName]
}
