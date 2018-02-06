package csw.services.event.impl

import akka.typed.ActorRef
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}

class EventSubscriberImpl extends EventSubscriber {

  override def getSubscription(callback: Event ⇒ Unit): EventSubscription = ???

  override def getSubscription(subscriberActor: ActorRef[Event]): EventSubscription = ???
}
