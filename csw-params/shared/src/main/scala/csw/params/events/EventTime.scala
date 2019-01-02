package csw.params.events

import csw.time.api.models.UTCTime
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

  implicit val reads: Reads[EventTime]   = implicitly[Reads[UTCTime]].map(EventTime.apply)
  implicit val writes: Writes[EventTime] = Writes[EventTime](x ⇒ implicitly[Writes[UTCTime]].writes(x.time))
}
