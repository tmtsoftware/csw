package example.event

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.Sink
import csw.command.client.messages.TopLevelActorMessage
import csw.event.api.scaladsl.{EventService, EventSubscription, SubscriptionModes}
import csw.location.api.models.AkkaLocation
import csw.params.events.{Event, EventKey, EventName}
import csw.prefix.models.Subsystem

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class EventSubscribeExamples(eventService: EventService, hcd: AkkaLocation)(implicit system: ActorSystem[_]) {

  def callback(): EventSubscription =
    // #with-callback
    {
      val subscriber = eventService.defaultSubscriber

      subscriber.subscribeCallback(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event => { /*do something*/ }
      )
    }
  // #with-callback

  // #with-async-callback
  def subscribe(): EventSubscription = {
    val subscriber = eventService.defaultSubscriber
    subscriber.subscribeAsync(Set(EventKey(hcd.prefix, EventName("filter_wheel"))), callback)
  }

  private def callback(event: Event): Future[String] = {
    /* do something */
    Future.successful("some value")
  }
  // #with-async-callback

  // #with-actor-ref
  def subscribe(ctx: ActorContext[TopLevelActorMessage]): EventSubscription = {
    val subscriber                    = eventService.defaultSubscriber
    val eventHandler: ActorRef[Event] = ctx.spawnAnonymous(EventHandler.behavior)

    subscriber.subscribeActorRef(Set(EventKey(hcd.prefix, EventName("filter_wheel"))), eventHandler)
  }

  object EventHandler {
    val behavior: Behavior[Event] = Behaviors.setup { ctx =>
      // setup required for the actor

      Behaviors.receiveMessage { case _ => // handle messages and return new behavior with changed state
        Behaviors.same
      }
    }
  }
  // #with-actor-ref

  def source(): EventSubscription =
    // #with-source
    {
      val subscriber = eventService.defaultSubscriber

      subscriber
        .subscribe(Set(EventKey(hcd.prefix, EventName("filter_wheel"))))
        .to(Sink.foreach { event => /*do something*/ })
        .run()
    }
  // #with-source

  def subscribeWithRate(): EventSubscription =
    // #with-subscription-mode
    {
      val subscriber = eventService.defaultSubscriber

      subscriber.subscribeCallback(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event => { /*do something*/ },
        1.seconds,
        SubscriptionModes.RateAdapterMode
      )
    }
  // #with-subscription-mode

  def subscribeToSubsystemEvents(subsystem: Subsystem): EventSubscription =
    // #psubscribe
    {
      val subscriber = eventService.defaultSubscriber
      subscriber.pSubscribeCallback(subsystem, "*", event => { /*do something*/ })
    }
  // #psubscribe

}
