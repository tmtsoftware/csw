package csw.messages.events

import csw.messages.params.models.Prefix
import play.api.libs.json.{Json, OFormat}

/**
 * A wrapper class representing the key for an event
 *
 * @param source
 * @param eventName
 */
case class EventKey(source: Prefix, eventName: EventName) {
  override def toString: String = s"${source.prefix}.$eventName"
}

object EventKey {
  private[messages] implicit val format: OFormat[EventKey] = Json.format[EventKey]
}
