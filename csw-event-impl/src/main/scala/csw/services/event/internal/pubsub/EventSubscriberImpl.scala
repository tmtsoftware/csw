package csw.services.event.internal.pubsub

import akka.Done
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{KillSwitch, Materializer}
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.api.EventSubscriberDriver
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}

import scala.concurrent.{ExecutionContext, Future}

class EventSubscriberImpl(eventSubscriberDriver: EventSubscriberDriver)(implicit val mat: Materializer, ec: ExecutionContext)
    extends EventSubscriber {

  def subscribe(eventKeys: Seq[EventKey]): Source[Event, EventSubscription] = {
    val keys = eventKeys.map(_.toString)

    eventSubscriberDriver
      .subscribe(keys)
      .map(x => Event.fromPb(x.value))
      .mapMaterializedValue(killSwitch => createSubscription(keys, killSwitch))
  }

  def subscribe(eventKeys: Seq[EventKey], callback: Event => Unit): EventSubscription = {
    subscribe(eventKeys).to(Sink.foreach(callback)).run()
  }

  def subscribe(eventKeys: Seq[EventKey], actorRef: ActorRef[Event]): EventSubscription = {
    subscribe(eventKeys, event => actorRef ! event)
  }

  private def createSubscription(keys: Seq[String], killSwitch: KillSwitch): EventSubscription =
    new EventSubscription {
      override def unsubscribe(): Future[Done] =
        eventSubscriberDriver
          .unsubscribe(keys)
          .transform(x ⇒ { killSwitch.shutdown(); x }, ex ⇒ { killSwitch.abort(ex); ex })

    }
}
