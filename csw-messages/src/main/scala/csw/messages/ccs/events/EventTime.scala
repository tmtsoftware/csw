package csw.messages.ccs.events

import java.time.{Clock, Instant}

import com.google.protobuf.timestamp.Timestamp
import csw.messages.params.pb.Implicits.instantMapper
import play.api.libs.json._

import scalapb.TypeMapper

case class EventTime(time: Instant) {
  override def toString: String = time.toString
}

object EventTime {

  def apply(): EventTime = new EventTime(Instant.now(Clock.systemUTC()))

  def toEventTime(time: Instant): EventTime = EventTime(time)

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
