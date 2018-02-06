package csw.services.event.scaladsl

import akka.typed.ActorRef
import csw.messages.ccs.events.Event

trait EventSubscriber {
  def getSubscription(callback: Event ⇒ Unit): EventSubscription
  def getSubscription(subscriberActor: ActorRef[Event]): EventSubscription
}
