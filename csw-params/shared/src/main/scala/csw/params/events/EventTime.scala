package csw.params.events

import java.time.Instant

import csw.time.api.UTCTime
import play.api.libs.json._

/**
 * A wrapper class representing the time of event creation
 *
 * @param time the instant stating the event creation
 */
case class EventTime(time: UTCTime) {
  override def toString: String = time.toString
}

object EventTime {

  /**
   * The apply method is used to create EventTime using Instant.now
   * As UTC timezone not supported in Scala.js, we removed the timezone. To be discussed more after time service.
   *
   * @return an EventTime representing event creation
   */
  def apply(): EventTime = new EventTime(UTCTime.now())

  implicit val instantFormat: Format[Instant] = new Format[Instant] {
    override def reads(json: JsValue): JsResult[Instant] = json match {
      case JsString(value) ⇒ JsSuccess(Instant.parse(value))
      case _               ⇒ JsError(s"can not parse $json into Instant")
    }

    override def writes(o: Instant): JsValue = JsString(o.toString)
  }

  implicit val reads: Reads[EventTime]   = implicitly[Reads[UTCTime]].map(EventTime.apply)
  implicit val writes: Writes[EventTime] = Writes[EventTime](x ⇒ implicitly[Writes[UTCTime]].writes(x.time))
}
