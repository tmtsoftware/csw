package csw.messages.events

import java.time.{Clock, Instant}

import play.api.libs.json._

/**
 * A wrapper class representing the time of event creation
 *
 * @param time the instant stating the event creation
 */
case class EventTime(time: Instant) {
  override def toString: String = time.toString
}

object EventTime {

  /**
   * The apply method is used to create EventTime using Instant.now in UTC timezone
   *
   * @return an EventTime representing event creation
   */
  def apply(): EventTime = new EventTime(Instant.now(Clock.systemUTC()))

  private[messages] implicit val format: Format[EventTime] = new Format[EventTime] {
    def writes(et: EventTime): JsValue            = JsString(et.toString)
    def reads(json: JsValue): JsResult[EventTime] = JsSuccess(EventTime(json.as[Instant]))
  }
}
