package csw.services.event.scaladsl

import csw.messages.ccs.events.EventKey

trait EventSubscription {

  def subscribe(eventKey: EventKey*): Unit

  def unsubscribe(): Unit

}
