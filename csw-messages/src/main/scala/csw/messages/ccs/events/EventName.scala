package csw.messages.ccs.events

import play.api.libs.json.{Json, OFormat}

/**
 * Model representing the name of an Event
 */
//TODO: add doc for why this model and how it gets used
case class EventName(name: String) {
  override def toString: String = name
}

object EventName {
  implicit val format: OFormat[EventName] = Json.format[EventName]
}
