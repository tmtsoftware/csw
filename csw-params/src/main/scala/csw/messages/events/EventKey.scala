package csw.messages.events

import csw.messages.params.models.Prefix
import play.api.libs.json.{Json, OFormat}

/**
 * A wrapper class representing the key for an event e.g. tcs.prog.cloudcover.oiwfsProbeDemands
 *
 * @param source represents the prefix of the component that publishes this event
 * @param eventName represents the name of the event
 */
case class EventKey(source: Prefix, eventName: EventName) {
  val key                       = s"${source.prefix}.$eventName"
  override def toString: String = key
}

object EventKey {
  private val SEPARATOR = "."

  def apply(eventKeyStr: String): EventKey = {
    require(eventKeyStr != null)
    val strings = eventKeyStr.splitAt(eventKeyStr.lastIndexOf(SEPARATOR))
    new EventKey(Prefix(strings._1), EventName(strings._2.tail))
  }

  private[messages] implicit val format: OFormat[EventKey] = Json.format[EventKey]
}
