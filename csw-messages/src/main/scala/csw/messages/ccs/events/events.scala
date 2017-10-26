package csw.messages.ccs.events

import java.util.Optional

import com.trueaccord.scalapb.TypeMapper
import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.{ObsId, Prefix}
import csw_messages_params.events.PbEvent
import csw_messages_params.events.PbEvent.PbEventType
import csw_messages_params.parameter.PbParameter

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

object EventType {
  private val mapper =
    TypeMapper[Seq[PbParameter], Set[Parameter[_]]] {
      _.map(Parameter.typeMapper2.toCustom).toSet
    } {
      _.map(Parameter.typeMapper2.toBase).toSeq
    }

  implicit def typeMapper[T <: EventType[_]]: TypeMapper[PbEvent, T] = new TypeMapper[PbEvent, T] {
    override def toCustom(base: PbEvent): T = {
      val factory: (EventInfo, Set[Parameter[_]]) ⇒ Any = base.eventType match {
        case PbEventType.StatusEvent      ⇒ StatusEvent.apply
        case PbEventType.ObserveEvent     ⇒ ObserveEvent.apply
        case PbEventType.SystemEvent      ⇒ SystemEvent.apply
        case PbEventType.Unrecognized(dd) ⇒ throw new RuntimeException(s"unknown event type=$dd")
      }
      factory(EventInfo(base.prefix), mapper.toCustom(base.paramSet)).asInstanceOf[T]
    }

    override def toBase(custom: T): PbEvent = {
      val pbEventType = custom match {
        case _: StatusEvent  ⇒ PbEventType.StatusEvent
        case _: ObserveEvent ⇒ PbEventType.ObserveEvent
        case _: SystemEvent  ⇒ PbEventType.SystemEvent
      }
      PbEvent()
        .withPrefix(custom.prefixStr)
        .withEventType(pbEventType)
        .withParamSet(mapper.toBase(custom.paramSet))
    }
  }
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
  def toPb: PbEvent                                      = EventType.typeMapper[StatusEvent].toBase(this)
}

object StatusEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): StatusEvent =
    new StatusEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): StatusEvent =
    new StatusEvent(info).madd(paramSet)

  def fromPb(pbEvent: PbEvent): StatusEvent = EventType.typeMapper[StatusEvent].toCustom(pbEvent)
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

  def toPb: PbEvent = EventType.typeMapper[ObserveEvent].toBase(this)
}

object ObserveEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): ObserveEvent =
    new ObserveEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): ObserveEvent =
    new ObserveEvent(info).madd(paramSet)

  def fromPb(pbEvent: PbEvent): ObserveEvent = EventType.typeMapper[ObserveEvent].toCustom(pbEvent)
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

  def toPb: PbEvent = EventType.typeMapper[SystemEvent].toBase(this)
}

object SystemEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): SystemEvent =
    new SystemEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): SystemEvent =
    new SystemEvent(info).madd(paramSet)

  def fromPb(pbEvent: PbEvent): SystemEvent = EventType.typeMapper[SystemEvent].toCustom(pbEvent)
}
