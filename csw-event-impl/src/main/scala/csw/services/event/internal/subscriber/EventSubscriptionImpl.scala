package csw.services.event.internal.subscriber

import akka.stream.scaladsl.Source
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.EventServiceCodec
import csw.services.event.scaladsl.EventSubscription
import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy

class EventSubscriptionImpl(callback: Event ⇒ Unit) extends EventSubscription {

  private val redisURI: RedisURI = RedisURI.create("redis://localhost/")

  val redisConnection: StatefulRedisPubSubConnection[String, PbEvent] =
    RedisClient.create(redisURI).connectPubSub(EventServiceCodec)

  val reactive: RedisPubSubReactiveCommands[String, PbEvent] = redisConnection.reactive()

  var eventKeys: Seq[EventKey] = Seq.empty

  override def subscribe(eventKey: EventKey*): Unit = {
    reactive.subscribe(eventKey.map(_.toString): _*).subscribe()

    eventKeys = eventKeys ++ eventKey

    Source
      .fromPublisher(reactive.observeChannels(OverflowStrategy.DROP))
      .runForeach(channelMessage ⇒ callback(Event.typeMapper[Event].toCustom(channelMessage.getMessage)))

  }

  override def unsubscribe(): Unit = reactive.unsubscribe(eventKeys.map(_.toString): _*)
}
