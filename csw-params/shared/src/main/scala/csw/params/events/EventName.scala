package csw.params.events

/**
 * A wrapper class representing the name of an Event
 */
case class EventName(name: String) {
  override def toString: String = name
}
