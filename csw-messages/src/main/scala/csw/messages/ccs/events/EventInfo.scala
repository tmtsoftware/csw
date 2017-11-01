package csw.messages.ccs.events

import java.util.UUID

import ai.x.play.json.Jsonx
import csw.messages.params.models.{ObsId, Prefix}

import scala.language.implicitConversions
import scala.runtime.ScalaRunTime._

/**
 * This will include information related to the observation that is related to an event.
 * This will grow and develop.
 *
 * @param source the source subsystem and prefix for the component
 * @param eventTime time of the event
 * @param obsId optional observation id
 * @param eventId automatically generated unique event id
 */
case class EventInfo(source: Prefix,
                     eventTime: EventTime,
                     obsId: Option[ObsId],
                     eventId: String = UUID.randomUUID().toString) {
  override def toString: String = s"$source: eId: $eventId, time: $eventTime, obsId: $obsId"

  override def equals(that: Any): Boolean = {
    that match {
      case that: EventInfo =>
        // Ignore the event ID && time to allow comparing events.  Is this right?
        this.source == that.source && this.obsId == that.obsId // && this.time == that.time
      case _ => false
    }
  }

  override def hashCode(): Int = _hashCode(this)
}

object EventInfo {
  implicit def apply(prefixStr: String): EventInfo = {
    val prefix: Prefix = Prefix(prefixStr)
    EventInfo(prefix, EventTime.toCurrent, None)
  }

  def apply(prefixStr: String, time: EventTime): EventInfo = {
    val prefix: Prefix = Prefix(prefixStr)
    EventInfo(prefix, time, None)
  }

  def apply(prefixStr: String, time: EventTime, obsId: ObsId): EventInfo = {
    val prefix: Prefix = Prefix(prefixStr)
    EventInfo(prefix, time, Some(obsId))
  }

  // Java APIs
  def create(prefix: String): EventInfo = EventInfo(prefix)

  implicit val eventInfoFormat = Jsonx.formatCaseClassUseDefaults[EventInfo]
}
