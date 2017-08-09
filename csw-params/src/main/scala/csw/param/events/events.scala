package csw.param.events

import java.util.Optional

import csw.param.models.{ObsId, Prefix}
import csw.param.generics.{Key, Parameter, ParameterSetKeyData, ParameterSetType}

import scala.compat.java8.OptionConverters.RichOptionForJava8

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
  def obsIdOption: Option[ObsId]     = info.obsId
  def obsIdOptional: Optional[ObsId] = info.obsId.asJava
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
case class StatusEvent(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[StatusEvent]
    with EventServiceEvent {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))
  def this(prefix: String, time: EventTime, obsId: ObsId) = this(EventInfo(prefix, time, obsId))

  override def create(data: Set[Parameter[_]]) = StatusEvent(info, data)

  // The following overrides are needed for the Java API and javadocs
  // (Using a Java interface caused various Java compiler errors)
  override def add[I <: Parameter[_]](param: I): StatusEvent = super.add(param)

  override def remove[S](key: Key[S]): StatusEvent = super.remove(key)
}

object StatusEvent {
  def apply(prefix: String, time: EventTime): StatusEvent = StatusEvent(EventInfo(prefix, time))

  def apply(prefix: String, time: EventTime, obsId: ObsId): StatusEvent =
    StatusEvent(EventInfo(Prefix(prefix), time, Some(obsId)))
}

/**
 * Defines a observe event
 *
 * @param info event related information
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class ObserveEvent(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[ObserveEvent]
    with EventServiceEvent {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))

  override def create(data: Set[Parameter[_]]) = ObserveEvent(info, data)

  // The following overrides are needed for the Java API and javadocs
  // (Using a Java interface caused various Java compiler errors)
  override def add[I <: Parameter[_]](param: I): ObserveEvent = super.add(param)

  override def remove[S](key: Key[S]): ObserveEvent = super.remove(key)
}

object ObserveEvent {
  def apply(prefix: String, time: EventTime): ObserveEvent = ObserveEvent(EventInfo(prefix, time))

  def apply(prefix: String, time: EventTime, obsId: ObsId): ObserveEvent =
    ObserveEvent(EventInfo(Prefix(prefix), time, Some(obsId)))
}

/**
 * Defines a system event
 *
 * @param info event related information
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class SystemEvent(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[SystemEvent]
    with EventServiceEvent {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))

  override def create(data: Set[Parameter[_]]) = SystemEvent(info, data)

  // The following overrides are needed for the Java API and javadocs
  // (Using a Java interface caused various Java compiler errors)
  override def add[I <: Parameter[_]](param: I): SystemEvent = super.add(param)

  override def remove[S](key: Key[S]): SystemEvent = super.remove(key)
}

object SystemEvent {
  def apply(prefix: String, time: EventTime): SystemEvent = SystemEvent(EventInfo(prefix, time))

  def apply(prefix: String, time: EventTime, obsId: ObsId): SystemEvent =
    SystemEvent(EventInfo(Prefix(prefix), time, Some(obsId)))
}
