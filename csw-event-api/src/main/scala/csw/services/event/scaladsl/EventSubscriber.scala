package csw.services.event.scaladsl

import akka.stream.scaladsl.Source
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}

trait EventSubscriber {
  def subscribe(eventKeys: Seq[EventKey]): Source[Event, EventSubscription]

  def subscribe(eventKeys: Seq[EventKey], callback: Event => Unit): EventSubscription

  def subscribe(eventKeys: Seq[EventKey], subscriberActor: ActorRef[Event]): EventSubscription
}
