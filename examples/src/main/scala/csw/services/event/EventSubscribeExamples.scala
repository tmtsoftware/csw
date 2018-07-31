package csw.services.event

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import csw.messages.TopLevelActorMessage
import csw.messages.events.{Event, EventKey, EventName}
import csw.messages.location.AkkaLocation
import csw.messages.params.models.Subsystem
import csw.services.event.api.scaladsl.{EventService, EventSubscription, SubscriptionModes}

import scala.async.Async._
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class EventSubscribeExamples(
    eventService: EventService,
    hcd: AkkaLocation
)(implicit ec: ExecutionContext, mat: Materializer) {

  def callback(): Future[Unit] =
    //#with-callback
    async {
      val subscriber = await(eventService.defaultSubscriber)

      subscriber.subscribeCallback(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event ⇒ { /*do something*/ }
      )
    }
  //#with-callback

  //#with-async-callback
  def subscribe(): Future[Unit] = async {
    val subscriber = await(eventService.defaultSubscriber)
    subscriber.subscribeAsync(Set(EventKey(hcd.prefix, EventName("filter_wheel"))), callback)
  }

  private def callback(event: Event): Future[String] = {
    /* do something */
    Future.successful("some value")
  }
  //#with-async-callback

  //#with-actor-ref
  def subscribe(ctx: ActorContext[TopLevelActorMessage]): Future[Unit] = async {
    val subscriber                    = await(eventService.defaultSubscriber)
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

  def source(): Unit =
    //#with-source
    async {
      val subscriber = await(eventService.defaultSubscriber)

      subscriber.subscribe(Set(EventKey(hcd.prefix, EventName("filter_wheel")))).runForeach(event ⇒ { /*do something*/ })
    }
  //#with-source

  def subscribeWithRate(): Future[EventSubscription] =
    //#with-subscription-mode
    async {
      val subscriber = await(eventService.defaultSubscriber)

      subscriber.subscribeCallback(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event ⇒ { /*do something*/ },
        1.seconds,
        SubscriptionModes.RateAdapterMode
      )
    }
  //#with-subscription-mode

  def subscribeToSubsystemEvents(subsystem: Subsystem): Future[EventSubscription] =
    // #psubscribe
    async {
      val subscriber = await(eventService.defaultSubscriber)
      subscriber.pSubscribeCallback(subsystem, "*", event ⇒ { /*do something*/ })
    }
  // #psubscribe

}
