package csw.services.event.scaladsl

import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}

import scala.concurrent.Future

trait EventSubscriber {
  def createSubscription(callback: Event ⇒ Unit, eventKeys: EventKey*): Future[EventSubscription]
  def createSubscription(subscriberActor: ActorRef[Event], eventKeys: EventKey*): Future[EventSubscription]
}
