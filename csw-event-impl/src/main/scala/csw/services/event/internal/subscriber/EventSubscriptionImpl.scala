package csw.services.event.internal.subscriber

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.scaladsl.EventSubscription
import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy

class EventSubscriptionImpl(
    reactiveCommands: RedisPubSubReactiveCommands[String, PbEvent],
    callback: Event ⇒ Unit,
    eventKeys: Seq[EventKey]
)(implicit val mat: Materializer)
    extends EventSubscription {

  reactiveCommands.subscribe(eventKeys.map(_.toString): _*).subscribe()

  Source
    .fromPublisher(reactiveCommands.observeChannels(OverflowStrategy.DROP))
    .runForeach(ch ⇒ callback(Event.fromPb(ch.getMessage)))

  override def unsubscribe(): Unit = reactiveCommands.unsubscribe(eventKeys.map(_.toString): _*)
}
