package csw.services.event.scaladsl

import akka.stream.scaladsl.Source
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw_protobuf.events.PbEvent

trait EventSubscriber {
  def subscribe(callback: Event ⇒ Unit, eventKeys: EventKey*): EventSubscription
  def subscribe(subscriberActor: ActorRef[Event], eventKeys: EventKey*): EventSubscription
  def subscribe(eventKeys: EventKey*): Source[EventMessage[String, PbEvent], EventSubscription]
}
