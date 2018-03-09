package csw.messages.events

import java.time.{Clock, Instant}

import com.google.protobuf.timestamp.Timestamp
import csw.messages.params.pb.Implicits.instantMapper
import play.api.libs.json._

import scalapb.TypeMapper

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

  /**
   * Creates an EventTime out of provided time
   *
   * @param time an Instant which is fed to create an EventTime
   * @return an instance of EventTime
   */
  def toEventTime(time: Instant): EventTime = EventTime(time)

  /**
   * Creates an EventTime out of current Instant
   *
   * @return an instance of EventTime
   */
  def toCurrent: EventTime = EventTime()

  implicit val format: Format[EventTime] = new Format[EventTime] {
    def writes(et: EventTime): JsValue            = JsString(et.toString)
    def reads(json: JsValue): JsResult[EventTime] = JsSuccess(EventTime.toEventTime(json.as[Instant]))
  }

  //used by Protobuf for conversion between Timestamp <==> EventTime
  implicit val typeMapper: TypeMapper[Timestamp, EventTime] =
    TypeMapper[Timestamp, EventTime] { x ⇒
      EventTime(instantMapper.toCustom(x))
    } { x ⇒
      instantMapper.toBase(x.time)
    }
}
