package csw.messages.ccs.events

import java.time.{Clock, Instant}

import play.api.libs.json._

import scala.language.implicitConversions

case class EventTime(time: Instant = Instant.now(Clock.systemUTC)) {
  override def toString: String = time.toString
}

object EventTime {

  implicit def toEventTime(time: Instant): EventTime = EventTime(time)
  implicit def toCurrent: EventTime                  = EventTime()
  implicit val format: Format[EventTime] = new Format[EventTime] {
    def writes(et: EventTime): JsValue            = JsString(et.toString)
    def reads(json: JsValue): JsResult[EventTime] = JsSuccess(json.as[Instant])
  }
}
