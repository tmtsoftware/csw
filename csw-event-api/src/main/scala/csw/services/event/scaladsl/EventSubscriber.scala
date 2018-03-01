package csw.services.event.scaladsl

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.ThrottlingStage

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait EventSubscriber {
  implicit protected def mat: Materializer

  def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription]

  def subscribe(eventKeys: Set[EventKey], every: FiniteDuration): Source[Event, EventSubscription] = {
    subscribe(eventKeys).via(new ThrottlingStage[Event](every))
  }

  def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_]): EventSubscription = {
    subscribe(eventKeys).mapAsync(1)(x => callback(x)).to(Sink.ignore).run()
  }

  def subscribeAsync(eventKeys: Set[EventKey], callback: Event => Future[_], every: FiniteDuration): EventSubscription = {
    subscribe(eventKeys, every).mapAsync(1)(x => callback(x)).to(Sink.ignore).run()
  }

  def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit): EventSubscription = {
    subscribe(eventKeys).to(Sink.foreach(callback)).run()
  }

  def subscribeCallback(eventKeys: Set[EventKey], callback: Event => Unit, every: FiniteDuration): EventSubscription = {
    subscribe(eventKeys, every).to(Sink.foreach(callback)).run()
  }

  def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event]): EventSubscription = {
    subscribeCallback(eventKeys, event => actorRef ! event)
  }

  def subscribeActorRef(eventKeys: Set[EventKey], actorRef: ActorRef[Event], every: FiniteDuration): EventSubscription = {
    subscribeCallback(eventKeys, event => actorRef ! event, every)
  }

  def get(eventKeys: Set[EventKey]): Future[Set[Event]]

  def get(eventKey: EventKey): Future[Event]
}
