package csw.services.event.internal.pubsub

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.api.EventBusDriver
import csw.services.event.scaladsl.{EventMessage, EventSubscriber, EventSubscription}
import csw_protobuf.events.PbEvent

import scala.concurrent.{ExecutionContext, Future}

class EventSubscriberImpl(eventBusDriver: EventBusDriver)(implicit val mat: Materializer, ec: ExecutionContext)
    extends EventSubscriber {

  def subscribe(eventKeys: EventKey*): Source[EventMessage[String, PbEvent], EventSubscription] = {
    val keyNames = eventKeys.map(_.toString)

    eventBusDriver.subscribe(keyNames: _*).mapMaterializedValue { killSwitch =>
      new EventSubscription {
        override def unsubscribe(): Future[Done] =
          eventBusDriver
            .unsubscribe(keyNames)
            .transform(x ⇒ { killSwitch.shutdown(); x }, ex ⇒ { killSwitch.abort(ex); ex })
      }
    }
  }

  def subscribe(callback: Event ⇒ Unit, eventKeys: EventKey*): EventSubscription = {
    subscribe(eventKeys: _*)
      .to(Sink.foreach(ch ⇒ callback(Event.fromPb(ch.value))))
      .run
  }

  def subscribe(actorRef: ActorRef[Event], eventKeys: EventKey*): EventSubscription = {
    subscribe(event => actorRef ! event, eventKeys: _*)
  }
}
