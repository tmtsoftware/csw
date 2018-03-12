package csw.messages.events

import play.api.libs.json.{Json, OFormat}

/**
 * A wrapper class representing the key for an event
 *
 * @param key is the combination of prefix and eventName
 */
case class EventKey(key: String) {
  override def toString: String = key
}

object EventKey {
  private[messages] implicit val format: OFormat[EventKey] = Json.format[EventKey]
}
