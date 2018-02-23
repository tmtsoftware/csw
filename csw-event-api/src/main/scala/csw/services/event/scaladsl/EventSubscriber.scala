package csw.services.event.scaladsl

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}

import scala.concurrent.Future

trait EventSubscriber {
  implicit protected def mat: Materializer

  def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription]

  def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription =
    subscribe(eventKeys).mapAsync(1)(x => callback(x)).to(Sink.ignore).run()

  def subscribe(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription =
    subscribe(eventKeys).to(Sink.foreach(callback)).run()

  def subscribe(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription =
    subscribe(eventKeys, event => actorRef ! event)
}
