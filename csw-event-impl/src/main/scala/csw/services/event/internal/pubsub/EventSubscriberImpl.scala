package csw.services.event.internal.pubsub

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{KillSwitch, Materializer}
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.api.EventBusDriver
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}

import scala.concurrent.ExecutionContext

class EventSubscriberImpl(eventBusDriver: EventBusDriver)(implicit val mat: Materializer, ec: ExecutionContext)
    extends EventSubscriber {

  def subscribe(eventKeys: Seq[EventKey]): Source[Event, EventSubscription] = {
    val keys = eventKeys.map(_.toString)

    eventBusDriver
      .subscribe(keys)
      .map(x => Event.fromPb(x.value))
      .mapMaterializedValue(killSwitch => createSubscription(keys, killSwitch))
  }

  def subscribe(callback: Event ⇒ Unit, eventKeys: Seq[EventKey]): EventSubscription = {
    subscribe(eventKeys).to(Sink.foreach(callback)).run
  }

  def subscribe(actorRef: ActorRef[Event], eventKeys: Seq[EventKey]): EventSubscription = {
    subscribe(event => actorRef ! event, eventKeys)
  }

  private def createSubscription(keys: Seq[String], killSwitch: KillSwitch): EventSubscription = { () =>
    eventBusDriver
      .unsubscribe(keys)
      .transform(x ⇒ { killSwitch.shutdown(); x }, ex ⇒ { killSwitch.abort(ex); ex })
  }
}
