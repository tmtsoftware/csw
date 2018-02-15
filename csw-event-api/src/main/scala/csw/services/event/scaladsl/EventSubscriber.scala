package csw.services.event.scaladsl

import akka.stream.scaladsl.Source
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw_protobuf.events.PbEvent

trait EventSubscriber {
  def subscribe(callback: Event ⇒ Unit, eventKeys: Seq[EventKey]): EventSubscription
  def subscribe(subscriberActor: ActorRef[Event], eventKeys: Seq[EventKey]): EventSubscription
  def subscribe(eventKeys: Seq[EventKey]): Source[Event, EventSubscription]
}
