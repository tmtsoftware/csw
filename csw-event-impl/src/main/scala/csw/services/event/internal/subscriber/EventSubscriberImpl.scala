package csw.services.event.internal.subscriber

import akka.typed.ActorRef
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}

class EventSubscriberImpl extends EventSubscriber {

  override def getSubscription(callback: Event ⇒ Unit): EventSubscription = new EventSubscriptionImpl(callback)

  override def getSubscription(subscriberActor: ActorRef[Event]): EventSubscription =
    new EventSubscriptionImpl(subscriberActor ! _)
}
