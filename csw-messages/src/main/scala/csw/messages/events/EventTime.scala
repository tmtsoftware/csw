package csw.messages.events

import java.time.{Clock, Instant}

import spray.json.{JsString, JsValue, JsonFormat}
import scala.language.implicitConversions

case class EventTime(time: Instant = Instant.now(Clock.systemUTC)) {
  override def toString: String = time.toString
}

object EventTime {
  import spray.json.DefaultJsonProtocol._

  implicit def toEventTime(time: Instant): EventTime = EventTime(time)
  implicit def toCurrent: EventTime                  = EventTime()
  implicit val format: JsonFormat[EventTime] = new JsonFormat[EventTime] {
    def write(et: EventTime): JsValue  = JsString(et.toString)
    def read(json: JsValue): EventTime = Instant.parse(json.convertTo[String])
  }

}
