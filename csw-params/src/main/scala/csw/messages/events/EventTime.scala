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

  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def reads(json: JsValue): JsResult[Instant] = json match {
      case JsString(value) ⇒ JsSuccess(Instant.parse(value))
      case _               ⇒ JsError(s"can not parse $json into Instant")
    }

    override def writes(o: Instant): JsValue = JsString(o.toString)
  }

  implicit val reads: Reads[EventTime]   = implicitly[Reads[Instant]].map(EventTime.apply)
  implicit val writes: Writes[EventTime] = Writes[EventTime](x ⇒ implicitly[Writes[Instant]].writes(x.time))
}
