package csw.services.models

import csw.messages.ccs.events.Event

case class EventChannel(event: Event) {

  def channel: String = event.source.prefix + "." + event.eventName.name

}
