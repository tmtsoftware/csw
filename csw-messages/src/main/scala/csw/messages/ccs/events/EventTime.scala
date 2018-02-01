package csw.messages.ccs.events

import java.time.{Clock, Instant}

import com.google.protobuf.timestamp.Timestamp
import scalapb.TypeMapper
import csw.messages.params.pb.Implicits.instantMapper
import play.api.libs.json._

import scala.language.implicitConversions

case class EventTime(time: Instant = Instant.now()) {
  override def toString: String = time.toString
}

object EventTime {

  implicit def toEventTime(time: Instant): EventTime = EventTime(time)
  implicit def toCurrent: EventTime                  = EventTime()
  implicit val format: Format[EventTime] = new Format[EventTime] {
    def writes(et: EventTime): JsValue            = JsString(et.toString)
    def reads(json: JsValue): JsResult[EventTime] = JsSuccess(json.as[Instant])
  }

  def apply(): EventTime = new EventTime()

  //used by Protobuf for conversion between Timestamp <==> EventTime
  implicit val typeMapper: TypeMapper[Timestamp, EventTime] =
    TypeMapper[Timestamp, EventTime] { x ⇒
      EventTime(instantMapper.toCustom(x))
    } { x ⇒
      instantMapper.toBase(x.time)
    }
}
