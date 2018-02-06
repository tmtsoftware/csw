package csw.services.event.scaladsl

import csw.messages.ccs.events.{Event, EventKey}

trait EventService {

  def publish(event: Event)

  def subscribeEvent(eventKey: EventKey)
}
