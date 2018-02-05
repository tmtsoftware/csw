package csw.services.event.scaladsl

import csw.messages.ccs.events.{Event, EventKey}

trait EventService {

  def publishEvent(event: Event)

  def subscribeEvent(eventKey: EventKey)
}
