package csw.param

import java.time.{Clock, Instant}
import java.util.UUID

import csw.param
import csw.param.parameters.{Key, Parameter}

import scala.language.implicitConversions

/**
 * Defines events used by the event and telemetry services
 */
object Events {
  import Parameters._

  case class EventTime(time: Instant = Instant.now(Clock.systemUTC)) {
    override def toString: String = time.toString
  }

  object EventTime {
    implicit def toEventTime(time: Instant): EventTime = EventTime(time)

    implicit def toCurrent = EventTime()
  }

  /**
   * Java API to get current time as EventTime
   */
  def getEventTime: EventTime = EventTime()

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
    override def toString = s"$source: eId: $eventId, time: $eventTime, obsId: $obsId"

    override def equals(that: Any): Boolean = {
      that match {
        case that: EventInfo =>
          // Ignore the event ID && time to allow comparing events.  Is this right?
          this.source == that.source && this.obsId == that.obsId // && this.time == that.time
        case _ => false
      }
    }
  }

  object EventInfo {
    implicit def apply(prefixStr: String): EventInfo = {
      val prefix: Prefix = prefixStr
      EventInfo(prefix, EventTime.toCurrent, None)
    }

    implicit def apply(prefixStr: String, time: EventTime): EventInfo = {
      val prefix: Prefix = prefixStr
      EventInfo(prefix, time, None)
    }

    implicit def apply(prefixStr: String, time: EventTime, obsId: ObsId): EventInfo = {
      val prefix: Prefix = prefixStr
      EventInfo(prefix, time, Some(obsId))
    }

    // Java APIs
    def create(prefix: String): EventInfo = EventInfo(prefix)
  }

  /**
   * Base trait for events
   *
   * @tparam T the subclass of EventType
   */
  sealed trait EventType[T <: EventType[T]] extends ParameterSetType[T] with ParameterSetKeyData { self: T =>

    /**
     * Contains related event information
     */
    def info: EventInfo

    override def prefix: Prefix = info.source

    /**
     * The event source is the prefix
     */
    def source: String = prefix.prefix

    /**
     * The time the event was created
     */
    def eventTime: EventTime = info.eventTime

    /**
     * The event id
     */
    def eventId: String = info.eventId

    /**
     * The observation ID
     */
    def obsIdOption: Option[param.ObsId] = info.obsId
  }

  /**
   * Type of event used in the event service
   */
  sealed trait EventServiceEvent {

    /**
     * The event's prefix and subsystem
     */
    def prefix: Prefix

    /**
     * The event's prefix as a string
     */
    def source: String
  }

  /**
   * Defines a status event
   *
   * @param info event related information
   * @param paramSet an optional initial set of parameters (keys with values)
   */
  case class StatusEvent(info: EventInfo, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends EventType[StatusEvent]
      with EventServiceEvent {

    // Java API
    def this(prefix: String) = this(EventInfo(prefix))

    override def create(data: ParameterSet) = StatusEvent(info, data)

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[I <: Parameter[_]](param: I): StatusEvent = super.add(param)

    override def remove[S, I <: Parameter[S]](key: Key[S, I]): StatusEvent = super.remove(key)
  }

  object StatusEvent {
    def apply(prefix: String, time: EventTime): StatusEvent = StatusEvent(EventInfo(prefix, time))

    def apply(prefix: String, time: EventTime, obsId: ObsId): StatusEvent =
      StatusEvent(EventInfo(prefix, time, Some(obsId)))
  }

  /**
   * Defines a observe event
   *
   * @param info event related information
   * @param paramSet an optional initial set of parameters (keys with values)
   */
  case class ObserveEvent(info: EventInfo, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends EventType[ObserveEvent]
      with EventServiceEvent {

    // Java API
    def this(prefix: String) = this(EventInfo(prefix))

    override def create(data: ParameterSet) = ObserveEvent(info, data)

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[I <: Parameter[_]](param: I): ObserveEvent = super.add(param)

    override def remove[S, I <: Parameter[S]](key: Key[S, I]): ObserveEvent = super.remove(key)
  }

  object ObserveEvent {
    def apply(prefix: String, time: EventTime): ObserveEvent = ObserveEvent(EventInfo(prefix, time))

    def apply(prefix: String, time: EventTime, obsId: ObsId): ObserveEvent =
      ObserveEvent(EventInfo(prefix, time, Some(obsId)))
  }

  /**
   * Defines a system event
   *
   * @param info event related information
   * @param paramSet an optional initial set of parameters (keys with values)
   */
  case class SystemEvent(info: EventInfo, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends EventType[SystemEvent]
      with EventServiceEvent {

    // Java API
    def this(prefix: String) = this(EventInfo(prefix))

    override def create(data: ParameterSet) = SystemEvent(info, data)

    // The following overrides are needed for the Java API and javadocs
    // (Using a Java interface caused various Java compiler errors)
    override def add[I <: Parameter[_]](param: I): SystemEvent = super.add(param)

    override def remove[S, I <: Parameter[S]](key: Key[S, I]): SystemEvent = super.remove(key)
  }

  object SystemEvent {
    def apply(prefix: String, time: EventTime): SystemEvent = SystemEvent(EventInfo(prefix, time))

    def apply(prefix: String, time: EventTime, obsId: ObsId): SystemEvent =
      SystemEvent(EventInfo(prefix, time, Some(obsId)))
  }
}
