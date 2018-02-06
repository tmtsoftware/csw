package csw.services.event.impl

import akka.typed.ActorRef
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import io.lettuce.core.RedisClient

class EventSubscriberImpl extends EventSubscriber {

  override def getSubscription(callback: Event ⇒ Unit): EventSubscription = new EventSubscriptionImpl(callback)

  override def getSubscription(subscriberActor: ActorRef[Event]): EventSubscription =
    new EventSubscriptionImpl(subscriberActor ! _)
}
