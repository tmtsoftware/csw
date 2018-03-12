package csw.messages.events

import play.api.libs.json.{Json, OFormat}

/**
 * A wrapper class representing the name of an Event
 */
case class EventName(name: String) {
  override def toString: String = name
}

object EventName {
  private[messages] implicit val format: OFormat[EventName] = Json.format[EventName]
}
