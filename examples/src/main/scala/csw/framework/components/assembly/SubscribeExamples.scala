package csw.framework.components.assembly

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import csw.messages.TopLevelActorMessage
import csw.messages.events.{Event, EventKey, EventName}
import csw.messages.location.AkkaLocation
import csw.messages.params.models.Subsystem
import csw.services.event.api.scaladsl.{EventService, SubscriptionModes}

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class SubscribeExamples(eventService: EventService) {

  def callback(hcd: AkkaLocation): Future[Unit] = async {
    //#with-callback

    val subscriber = await(eventService.defaultSubscriber)

    subscriber.subscribeCallback(
      Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
      event ⇒ { /*do something*/ }
    )

    //#with-callback
  }

  def asyncCallback(hcd: AkkaLocation): Future[Unit] = async {
    //#with-async-callback

    val subscriber = await(eventService.defaultSubscriber)

    subscriber.subscribeAsync(
      Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
      event ⇒ Future.successful { /*do something*/ }
    )

    //#with-async-callback
  }

  def actorRef(hcd: AkkaLocation, ctx: ActorContext[TopLevelActorMessage]): Future[Unit] = async {
    //#with-actor-ref

    val subscriber                    = await(eventService.defaultSubscriber)
    val eventHandler: ActorRef[Event] = ctx.spawnAnonymous(EventHandler.make())

    subscriber.subscribeActorRef(Set(EventKey(hcd.prefix, EventName("filter_wheel"))), eventHandler)

    //#with-actor-ref
  }

  def source(hcd: AkkaLocation): Future[Unit] = async {
    //#with-source

    val subscriber = await(eventService.defaultSubscriber)
    subscriber.subscribe(Set(EventKey(hcd.prefix, EventName("filter_wheel")))).runForeach(event ⇒ { /*do something*/ })

    //#with-source
  }

  def subscriptionMode(hcd: AkkaLocation): Future[Unit] = async {
    //#with-subscription-mode

    val subscriber = await(eventService.defaultSubscriber)

    subscriber.subscribeCallback(
      Set(EventKey(hcd.prefix, EventName("filter_wheel"))),
      event ⇒ { /*do something*/ },
      1.seconds,
      SubscriptionModes.RateAdapterMode
    )

    //#with-subscription-mode
  }

  // #psubscribe
  private def subscribeToSubsystemEvents(subsystem: Subsystem) = async {
    val subscriber = await(eventService.defaultSubscriber)
    subscriber.pSubscribeCallback(subsystem, "*", callback)
  }
  // #psubscribe

  private def callback(event: Event): Unit = {
    //do something
  }

}
