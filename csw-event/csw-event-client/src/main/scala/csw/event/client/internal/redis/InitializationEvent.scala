package csw.event.client.internal.redis
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.Subsystem
import csw.params.events.{EventKey, SystemEvent}

private[event] object InitializationEvent {

  private val initParam = StringKey.make("InitKey").set("IGNORE: Redis publisher initialization")
  private val prefix    = s"${Subsystem.TEST}.first.event"
  private val eventKey  = EventKey(s"$prefix.init")

  def value: SystemEvent = SystemEvent(eventKey.source, eventKey.eventName, Set(initParam))

}
