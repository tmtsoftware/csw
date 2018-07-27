package csw.services.event

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.stream.Materializer
import csw.framework.components.assembly.EventHandler
import csw.messages.TopLevelActorMessage
import csw.messages.events.{Event, EventKey, EventName}
import csw.messages.location.AkkaLocation
import csw.messages.params.models.Subsystem
import csw.services.event.api.scaladsl.{EventService, EventSubscription, SubscriptionModes}

import scala.async.Async._
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class EventSubscribeExamples(
    eventService: EventService
)(implicit ec: ExecutionContext, mat: Materializer) {

  def callback(hcd: AkkaLocation): Future[Unit] =
    //#with-callback
    async {
      val subscriber = await(eventService.defaultSubscriber)

      subscriber.subscribeCallback(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event ⇒ { /*do something*/ }
      )
    }
  //#with-callback

  def asyncCallback(hcd: AkkaLocation): Future[Unit] =
    //#with-async-callback
    async {
      val subscriber = await(eventService.defaultSubscriber)

      subscriber.subscribeAsync(
        Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
        event ⇒ Future.successful { /*do something*/ }
      )
    }
  //#with-async-callback

  def actorRef(hcd: AkkaLocation, ctx: ActorContext[TopLevelActorMessage]): Future[Unit] =
    //#with-actor-ref
    async {
      val subscriber                    = await(eventService.defaultSubscriber)
      val eventHandler: ActorRef[Event] = ctx.spawnAnonymous(EventHandler.make())

      subscriber.subscribeActorRef(Set(EventKey(hcd.prefix, EventName("filter_wheel"))), eventHandler)
    }
  //#with-actor-ref

  def source(hcd: AkkaLocation): Unit =
    //#with-source
    async {
      val subscriber = await(eventService.defaultSubscriber)

      subscriber.subscribe(Set(EventKey(hcd.prefix, EventName("filter_wheel")))).runForeach(event ⇒ { /*do something*/ })
    }
  //#with-source

  def subscribeWithRate(hcd: AkkaLocation): Future[EventSubscription] =
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
