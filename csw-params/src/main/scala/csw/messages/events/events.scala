package csw.messages.events

import java.time.Instant

import csw.messages.params.generics.{Parameter, ParameterSetType}
import csw.messages.params.models.{Id, Prefix, Subsystem}

/**
 * Common trait representing events in TMT like [[csw.messages.events.SystemEvent]] and [[csw.messages.events.ObserveEvent]]
 */
sealed trait Event { self: ParameterSetType[_] ⇒

  /**
   * A helper to give access of public members of ParameterSetType
   *
   * @return a handle to ParameterSetType extended by concrete implementation of this class
   */
  def paramType: ParameterSetType[_] = self

  /**
   * unique Id for event
   */
  val eventId: Id

  /**
   * Prefix representing source of the event
   */
  val source: Prefix

  /**
   * The name of event
   */
  val eventName: EventName

  /**
   * The time of event creation
   */
  val eventTime: EventTime

  /**
   * An optional initial set of parameters (keys with values)
   */
  val paramSet: Set[Parameter[_]]

  /**
   * A name identifying the type of parameter set, such as "SystemEvent", "ObserveEvent".
   * This is used in the JSON and toString output.
   *
   * @return a string representation of concrete type of this class
   */
  def typeName: String

  /**
   * The EventKey used to publish or subscribe an event
   *
   * @return an EventKey formed by combination of prefix and eventName of an event
   */
  def eventKey: EventKey = EventKey(source, eventName)

  /**
   * A common toString method for all concrete implementation
   *
   * @return the string representation of command
   */
  override def toString: String =
    s"$typeName(eventId=$eventId, source=$source, eventName=$eventName, eventTime=$eventTime, paramSet=$paramSet)"

  def isInvalid: Boolean = eventTime == EventTime(Instant.ofEpochMilli(-1))
}

object Event {

  /**
   * A helper method to create an event which is provided to subscriber when there is no event available at the
   * time of subscription
   * @param eventKey the Event Key for which subscription was made
   * @return an event with the same key as provided but with id and timestamp denoting an invalid event
   */
  def invalidEvent(eventKey: EventKey): SystemEvent =
    SystemEvent(eventKey.source, eventKey.eventName)
      .copy(eventId = Id("-1"), eventTime = EventTime(Instant.ofEpochMilli(-1)))

  /**
   * A helper method to create an event which is provided to subscriber when the received bytes could not be
   * decoded into a valid event
   * @return an invalid event with the key representing a bad key by using a BAD subsystem
   */
  def badEvent(): SystemEvent = Event.invalidEvent(EventKey(s"${Subsystem.BAD}.CouldNotParseEvent"))
}

/**
 * Defines a system event. Constructor is private to ensure eventId is created internally to guarantee unique value.
 */
case class SystemEvent private (
    eventId: Id,
    source: Prefix,
    eventName: EventName,
    eventTime: EventTime,
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[SystemEvent]
    with Event {

  /**
   * A java helper to construct SystemEvent
   */
  def this(source: Prefix, eventName: EventName) = this(Id(), source, eventName, EventTime(), Set.empty)

  /**
   * Create a new SystemEvent instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of SystemEvent with new eventId, eventTime and provided data
   */
  override protected def create(data: Set[Parameter[_]]): SystemEvent =
    copy(eventId = Id(), eventTime = EventTime(), paramSet = data)
}

object SystemEvent {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to SystemEvent model
  private[messages] def apply(
      eventId: Id,
      source: Prefix,
      eventName: EventName,
      eventTime: EventTime,
      paramSet: Set[Parameter[_]]
  ) = new SystemEvent(eventId, source, eventName, eventTime, paramSet)

  /**
   * The apply method is used to create SystemEvent command by end-user. eventId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the event
   * @param eventName the name of event
   * @return a new instance of SystemEvent with auto-generated eventId, eventTime and empty paramSet
   */
  def apply(source: Prefix, eventName: EventName): SystemEvent = apply(Id(), source, eventName, EventTime(), Set.empty)

  /**
   * The apply method is used to create SystemEvent command by end-user. eventId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the event
   * @param eventName the name of event
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of SystemEvent with auto-generated eventId and eventTime
   */
  def apply(source: Prefix, eventName: EventName, paramSet: Set[Parameter[_]]): SystemEvent =
    apply(source, eventName).madd(paramSet)
}

/**
 * Defines an observe event. Constructor is private to ensure eventId is created internally to guarantee unique value.
 */
case class ObserveEvent private (
    eventId: Id,
    source: Prefix,
    eventName: EventName,
    eventTime: EventTime,
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[ObserveEvent]
    with Event {

  /**
   * A java helper to construct ObserveEvent
   */
  def this(source: Prefix, eventName: EventName) = this(Id(), source, eventName, EventTime(), Set.empty)

  /**
   * Create a new ObserveEvent instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of ObserveEvent with new eventId, eventTime and provided data
   */
  override protected def create(data: Set[Parameter[_]]): ObserveEvent =
    copy(eventId = Id(), eventTime = EventTime(), paramSet = data)

  /**
 * A helper method to create PbEvent out of this Event
 *
 * @return a protobuf representation of ObserveEvent
 */

}

object ObserveEvent {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to ObserveEvent model
  private[messages] def apply(
      eventId: Id,
      source: Prefix,
      eventName: EventName,
      eventTime: EventTime,
      paramSet: Set[Parameter[_]]
  ) = new ObserveEvent(eventId, source, eventName, eventTime, paramSet)

  /**
   * The apply method is used to create ObserveEvent command by end-user. eventId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the event
   * @param eventName the name of event
   * @return a new instance of ObserveEvent with auto-generated eventId, eventTime and empty paramSet
   */
  def apply(source: Prefix, eventName: EventName): ObserveEvent = apply(Id(), source, eventName, EventTime(), Set.empty)

  /**
   * The apply method is used to create ObserveEvent command by end-user. eventId is not accepted and will be created internally to guarantee unique value.
   *
   * @param source prefix representing source of the event
   * @param eventName the name of event
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of ObserveEvent with auto-generated eventId and eventTime
   */
  def apply(source: Prefix, eventName: EventName, paramSet: Set[Parameter[_]]): ObserveEvent =
    apply(source, eventName).madd(paramSet)
}
