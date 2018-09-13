package csw.services.event

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import csw.command.messages.TopLevelActorMessage
import csw.messages.events.{Event, EventKey, EventName}
import csw.services.location.api.models.AkkaLocation
import csw.messages.params.models.Subsystem
import csw.services.event.api.scaladsl.{EventService, EventSubscription, SubscriptionModes}

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class EventSubscribeExamples(eventService: EventService, hcd: AkkaLocation)(implicit mat: Materializer) {

  def callback(): EventSubscription =
    //#with-callback
    {
      val subscriber = eventService.defaultSubscriber

      subscriber.subscribeCallback(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event ⇒ { /*do something*/ }
      )
    }
  //#with-callback

  //#with-async-callback
  def subscribe(): EventSubscription = {
    val subscriber = eventService.defaultSubscriber
    subscriber.subscribeAsync(Set(EventKey(hcd.prefix, EventName("filter_wheel"))), callback)
  }

  private def callback(event: Event): Future[String] = {
    /* do something */
    Future.successful("some value")
  }
  //#with-async-callback

  //#with-actor-ref
  def subscribe(ctx: ActorContext[TopLevelActorMessage]): EventSubscription = {
    val subscriber                    = eventService.defaultSubscriber
    val eventHandler: ActorRef[Event] = ctx.spawnAnonymous(EventHandler.make())

    subscriber.subscribeActorRef(Set(EventKey(hcd.prefix, EventName("filter_wheel"))), eventHandler)
  }

  object EventHandler {
    def make(): Behavior[Event] = Behaviors.setup(ctx ⇒ new EventHandler(ctx))
  }

  class EventHandler(ctx: ActorContext[Event]) extends MutableBehavior[Event] {
    override def onMessage(msg: Event): Behavior[Event] = {
      // handle messages
      Behaviors.same
    }
  }
  //#with-actor-ref

  def source(): EventSubscription =
    //#with-source
    {
      val subscriber = eventService.defaultSubscriber

      subscriber
        .subscribe(Set(EventKey(hcd.prefix, EventName("filter_wheel"))))
        .to(Sink.foreach { event => /*do something*/
        })
        .run()
    }
  //#with-source

  def subscribeWithRate(): EventSubscription =
    //#with-subscription-mode
    {
      val subscriber = eventService.defaultSubscriber

      subscriber.subscribeCallback(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event ⇒ { /*do something*/ },
        1.seconds,
        SubscriptionModes.RateAdapterMode
      )
    }
  //#with-subscription-mode

  def subscribeToSubsystemEvents(subsystem: Subsystem): EventSubscription =
    // #psubscribe
    {
      val subscriber = eventService.defaultSubscriber
      subscriber.pSubscribeCallback(subsystem, "*", event ⇒ { /*do something*/ })
    }
  // #psubscribe

}
