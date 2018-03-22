package csw.services.event.scaladsl

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.{Event, EventKey}
import csw.services.event.internal.RateAdapterStage

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait EventSubscriber {
  implicit protected def mat: Materializer

  def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription]

  def subscribe(eventKeys: Set[EventKey], every: FiniteDuration): Source[Event, EventSubscription] = {
    subscribe(eventKeys).via(new RateAdapterStage[Event](every))
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

//  def subscribeWithSinkActorRef(eventKeys: Set[EventKey], every: FiniteDuration): Source[Event, EventSubscription] = {
//    implicit val timeout: Timeout       = Timeout(10.seconds)
//    implicit val sender: actor.ActorRef = DummyActor()
//
//    val eventStoreActor   = EventStoreActor()
//    val eventSubscription = subscribe(eventKeys).to(Sink.actorRef(eventStoreActor, "streamCompleted")).run()
//
//    def event(): Event = {
//      val eventualEvent = eventStoreActor ? "getLatest"
//      Await.result(eventualEvent, every).asInstanceOf[Event]
//    }
//    Source
//      .tick(0.millis, every, ())
//      .map(_ ⇒ event())
//      .mapMaterializedValue(_ ⇒ eventSubscription)
//  }
}
