package csw.param.events

import java.util.Optional

import csw.param.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.param.models.params.{ObsId, Prefix}

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
case class StatusEvent private (info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[StatusEvent]
    with EventServiceEvent {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))
  def this(prefix: String, time: EventTime, obsId: ObsId) = this(EventInfo(prefix, time, obsId))

  override protected def create(data: Set[Parameter[_]]) = new StatusEvent(info, data)
}

object StatusEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): StatusEvent =
    new StatusEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): StatusEvent =
    new StatusEvent(info).madd(paramSet)
}

/**
 * Defines a observe event
 *
 * @param info event related information
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class ObserveEvent private (info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[ObserveEvent]
    with EventServiceEvent {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))

  override protected def create(data: Set[Parameter[_]]) = new ObserveEvent(info, data)
}

object ObserveEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): ObserveEvent =
    new ObserveEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): ObserveEvent =
    new ObserveEvent(info).madd(paramSet)
}

/**
 * Defines a system event
 *
 * @param info event related information
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class SystemEvent private (info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[SystemEvent]
    with EventServiceEvent {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))

  override protected def create(data: Set[Parameter[_]]) = new SystemEvent(info, data)
}

object SystemEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): SystemEvent =
    new SystemEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): SystemEvent =
    new SystemEvent(info).madd(paramSet)
}
