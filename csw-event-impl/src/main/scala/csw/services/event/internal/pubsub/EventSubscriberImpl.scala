package csw.services.event.internal.pubsub

import akka.Done
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{KillSwitch, Materializer}
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.api.{EventSubscriberDriver, EventSubscriberDriverFactory}
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}

import scala.concurrent.{ExecutionContext, Future}

class EventSubscriberImpl(eventSubscriberDriverFactory: EventSubscriberDriverFactory)(implicit val mat: Materializer,
                                                                                      ec: ExecutionContext)
    extends EventSubscriber {

  def subscribe(eventKeys: Seq[EventKey]): Source[Event, EventSubscription] = {
    val subscriberDriver = eventSubscriberDriverFactory.make()

    subscriberDriver
      .subscribe(eventKeys)
      .map(x => x.value)
      .mapMaterializedValue(killSwitch => createSubscription(eventKeys, killSwitch, subscriberDriver))
  }

  def subscribe(eventKeys: Seq[EventKey], callback: Event => Unit): EventSubscription = {
    subscribe(eventKeys).to(Sink.foreach(callback)).run()
  }

  def subscribe(eventKeys: Seq[EventKey], actorRef: ActorRef[Event]): EventSubscription = {
    subscribe(eventKeys, event => actorRef ! event)
  }

  private def createSubscription(eventKeys: Seq[EventKey],
                                 killSwitch: KillSwitch,
                                 subscriberDriver: EventSubscriberDriver): EventSubscription =
    new EventSubscription {
      override def unsubscribe(): Future[Done] =
        subscriberDriver
          .unsubscribe(eventKeys)
          .transform(x ⇒ { killSwitch.shutdown(); x }, ex ⇒ { killSwitch.abort(ex); ex })

    }
}
