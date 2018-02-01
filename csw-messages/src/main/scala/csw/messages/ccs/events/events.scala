package csw.messages.ccs.events

import csw.messages.params.generics.{Parameter, ParameterSetType}
import csw.messages.params.models.{Id, Prefix}
import csw_protobuf.events.PbEvent
import csw_protobuf.events.PbEvent.PbEventType
import csw_protobuf.parameter.PbParameter

import scalapb.TypeMapper

sealed trait Event {
  self: ParameterSetType[_] ⇒

  def paramType: ParameterSetType[_] = self

  val eventId: Id

  val source: Prefix

  val name: String

  val eventTime: EventTime

  val paramSet: Set[Parameter[_]]
}

object Event {
  private val mapper =
    TypeMapper[Seq[PbParameter], Set[Parameter[_]]] {
      _.map(Parameter.typeMapper2.toCustom).toSet
    } {
      _.map(Parameter.typeMapper2.toBase).toSeq
    }

  /**
   * TypeMapper definitions are required for to/from conversion PbEvent(Protobuf) <==> System, Observe event.
   */
  implicit def typeMapper[T <: Event]: TypeMapper[PbEvent, T] = new TypeMapper[PbEvent, T] {
    override def toCustom(base: PbEvent): T = {
      val factory: (Id, Prefix, String, EventTime, Set[Parameter[_]]) ⇒ Any = base.eventType match {
        case PbEventType.SystemEvent      ⇒ SystemEvent.apply
        case PbEventType.ObserveEvent     ⇒ ObserveEvent.apply
        case PbEventType.Unrecognized(dd) ⇒ throw new RuntimeException(s"unknown event type=$dd")
      }

      factory(
        Id(base.eventId),
        Prefix(base.source),
        base.name,
        base.eventTime.map(EventTime.typeMapper.toCustom).get,
        mapper.toCustom(base.paramSet)
      ).asInstanceOf[T]
    }

    override def toBase(custom: T): PbEvent = {
      val pbEventType = custom match {
        case _: ObserveEvent ⇒ PbEventType.ObserveEvent
        case _: SystemEvent  ⇒ PbEventType.SystemEvent
      }
      PbEvent()
        .withEventId(custom.eventId.id)
        .withSource(custom.source.prefix)
        .withName(custom.name)
        .withEventTime(EventTime.typeMapper.toBase(custom.eventTime))
        .withParamSet(mapper.toBase(custom.paramSet))
        .withEventType(pbEventType)
    }
  }
}

/**
 * Defines a observe event
 *
 * @param eventId
 * @param source
 * @param name
 * @param eventTime
 * @param paramSet
 */
case class ObserveEvent private (eventId: Id, source: Prefix, name: String, eventTime: EventTime, paramSet: Set[Parameter[_]])
    extends ParameterSetType[ObserveEvent]
    with Event {

  def this(source: Prefix, name: String) = this(Id(), source, name, EventTime(), Set.empty) //   Java API
  override protected def create(data: Set[Parameter[_]]): ObserveEvent = copy(paramSet = data)

  /**
   * Returns Protobuf representation of ObserveEvent
   */
  def toPb: Array[Byte] = Event.typeMapper[ObserveEvent].toBase(this).toByteArray
}

object ObserveEvent {

  private[messages] def apply(
      eventId: Id,
      source: Prefix,
      name: String,
      eventTime: EventTime,
      paramSet: Set[Parameter[_]]
  ): ObserveEvent = new ObserveEvent(eventId, source, name, eventTime, paramSet)

  def apply(source: Prefix, eventName: String): ObserveEvent = apply(Id(), source, eventName, EventTime(), Set.empty)

  def apply(source: Prefix, eventName: String, paramSet: Set[Parameter[_]]): ObserveEvent =
    apply(source, eventName).madd(paramSet)

  /**
   * Constructs from byte array containing Protobuf representation of ObserveEvent
   */
  def fromPb(array: Array[Byte]): ObserveEvent = Event.typeMapper[ObserveEvent].toCustom(PbEvent.parseFrom(array))
}

/**
 * Defines a system event
 *
 * @param eventId
 * @param source
 * @param name
 * @param eventTime
 * @param paramSet
 */
case class SystemEvent private (eventId: Id, source: Prefix, name: String, eventTime: EventTime, paramSet: Set[Parameter[_]])
    extends ParameterSetType[SystemEvent]
    with Event {

  def this(source: Prefix, name: String) = this(Id(), source, name, EventTime(), Set.empty) // Java API
  override protected def create(data: Set[Parameter[_]]): SystemEvent = copy(paramSet = data)

  /**
   * Returns Protobuf representation of SystemEvent
   */
  def toPb: Array[Byte] = Event.typeMapper[SystemEvent].toBase(this).toByteArray
}

object SystemEvent {
  def apply(source: Prefix, eventName: String): SystemEvent = apply(Id(), source, eventName, EventTime(), Set.empty)

  def apply(source: Prefix, eventName: String, paramSet: Set[Parameter[_]]): SystemEvent =
    apply(source, eventName).madd(paramSet)

  /**
   * Constructs from byte array containing Protobuf representation of SystemEvent
   */
  def fromPb(array: Array[Byte]): SystemEvent = Event.typeMapper[SystemEvent].toCustom(PbEvent.parseFrom(array))
}
