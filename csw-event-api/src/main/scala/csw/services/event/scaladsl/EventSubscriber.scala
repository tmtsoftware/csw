package csw.services.event.scaladsl

import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}

trait EventSubscriber {
  def createSubscription(callback: Event ⇒ Unit, eventKeys: EventKey*): EventSubscription
  def createSubscription(subscriberActor: ActorRef[Event], eventKeys: EventKey*): EventSubscription
}
