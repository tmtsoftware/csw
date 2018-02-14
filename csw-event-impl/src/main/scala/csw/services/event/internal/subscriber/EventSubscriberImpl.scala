package csw.services.event.internal.subscriber

import akka.stream.Materializer
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.scaladsl.{EventServiceDriver, EventSubscriber, EventSubscription}

import scala.async.Async.async
import scala.concurrent.{ExecutionContext, Future}

class EventSubscriberImpl(eventServiceDriver: EventServiceDriver)(implicit val mat: Materializer, ec: ExecutionContext)
    extends EventSubscriber {

  override def createSubscription(callback: Event ⇒ Unit, eventKeys: EventKey*): Future[EventSubscription] = async {
    new EventSubscriptionImpl(eventServiceDriver, callback, eventKeys)
  }

  override def createSubscription(subscriberActor: ActorRef[Event], eventKeys: EventKey*): Future[EventSubscription] = async {
    new EventSubscriptionImpl(eventServiceDriver, subscriberActor ! _, eventKeys)
  }
}
