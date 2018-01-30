package csw.messages.ccs.events

import java.util.Optional

import scalapb.TypeMapper
import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.{ObsId, Prefix}
import csw_protobuf.events.PbEvent
import csw_protobuf.events.PbEvent.PbEventType
import csw_protobuf.parameter.PbParameter

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

  /**
   * TypeMapper definitions are required for to/from conversion PbEvent(Protobuf) <==> System, Observe, Status event.
   */
  implicit def typeMapper[T <: EventType[_]]: TypeMapper[PbEvent, T] = new TypeMapper[PbEvent, T] {
    override def toCustom(base: PbEvent): T = {
      val factory: (EventInfo, Set[Parameter[_]]) ⇒ Any = base.eventType match {
        case PbEventType.ObserveEvent     ⇒ ObserveEvent.apply
        case PbEventType.SystemEvent      ⇒ SystemEvent.apply
        case PbEventType.Unrecognized(dd) ⇒ throw new RuntimeException(s"unknown event type=$dd")
      }

      factory(
        EventInfo(Prefix(base.prefix),
                  base.eventTime.map(EventTime.typeMapper.toCustom).get,
                  ObsId.mapper.toCustom(base.obsId),
                  base.eventId),
        mapper.toCustom(base.paramSet)
      ).asInstanceOf[T]
    }

    override def toBase(custom: T): PbEvent = {
      val pbEventType = custom match {
        case _: ObserveEvent ⇒ PbEventType.ObserveEvent
        case _: SystemEvent  ⇒ PbEventType.SystemEvent
      }
      PbEvent()
        .withPrefix(custom.prefixStr)
        .withEventType(pbEventType)
        .withEventTime(EventTime.typeMapper.toBase(custom.eventTime))
        .withObsId(ObsId.mapper.toBase(custom.obsIdOption))
        .withEventId(custom.eventId)
        .withParamSet(mapper.toBase(custom.paramSet))
    }
  }
}

/**
 * Type of event used in the event service
 */
sealed trait Event {

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
 * Defines a observe event
 *
 * @param info event related information
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class ObserveEvent private (info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[ObserveEvent]
    with Event {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))
  def this(prefix: String, time: EventTime, obsId: ObsId) = this(EventInfo(prefix, time, obsId))

  override protected def create(data: Set[Parameter[_]]) = new ObserveEvent(info, data)

  /**
   * Returns Protobuf representation of ObserveEvent
   */
  def toPb: Array[Byte] = EventType.typeMapper[ObserveEvent].toBase(this).toByteArray
}

object ObserveEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): ObserveEvent =
    new ObserveEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): ObserveEvent =
    new ObserveEvent(info).madd(paramSet)

  /**
   * Constructs ObserveEvent from EventInfo
   */
  def from(info: EventInfo): ObserveEvent = new ObserveEvent(info)

  /**
   * Constructs from byte array containing Protobuf representation of ObserveEvent
   */
  def fromPb(array: Array[Byte]): ObserveEvent = EventType.typeMapper[ObserveEvent].toCustom(PbEvent.parseFrom(array))
}

/**
 * Defines a system event
 *
 * @param info event related information
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class SystemEvent private (info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends EventType[SystemEvent]
    with Event {

  // Java API
  def this(prefix: String) = this(EventInfo(prefix))
  def this(prefix: String, time: EventTime, obsId: ObsId) = this(EventInfo(prefix, time, obsId))

  override protected def create(data: Set[Parameter[_]]) = new SystemEvent(info, data)

  /**
   * Returns Protobuf representation of SystemEvent
   */
  def toPb: Array[Byte] = EventType.typeMapper[SystemEvent].toBase(this).toByteArray
}

object SystemEvent {
  def apply(prefix: String, time: EventTime, obsId: ObsId): SystemEvent =
    new SystemEvent(EventInfo(Prefix(prefix), time, Some(obsId)))

  def apply(info: EventInfo, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): SystemEvent =
    new SystemEvent(info).madd(paramSet)

  /**
   * Constructs SystemEvent from EventInfo
   */
  def from(info: EventInfo): SystemEvent = new SystemEvent(info)

  /**
   * Constructs from byte array containing Protobuf representation of SystemEvent
   */
  def fromPb(array: Array[Byte]): SystemEvent = EventType.typeMapper[SystemEvent].toCustom(PbEvent.parseFrom(array))
}
