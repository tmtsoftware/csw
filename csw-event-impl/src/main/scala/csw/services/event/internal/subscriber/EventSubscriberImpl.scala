package csw.services.event.internal.subscriber

import akka.stream.Materializer
import akka.typed.ActorRef
import csw.messages.ccs.events.{Event, EventKey}
import csw.services.event.internal.EventServiceCodec
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class EventSubscriberImpl(implicit val mat: Materializer, ec: ExecutionContext) extends EventSubscriber {

  val redisURI: RedisURI = RedisURI.create("redis://localhost/")

  val redisConnectionF: Future[StatefulRedisPubSubConnection[String, PbEvent]] =
    RedisClient.create(redisURI).connectPubSubAsync(EventServiceCodec, redisURI).toScala

  val reactiveCommandsF: Future[RedisPubSubReactiveCommands[String, PbEvent]] = redisConnectionF.map(_.reactive())

  override def createSubscription(callback: Event ⇒ Unit, eventKeys: EventKey*): Future[EventSubscription] = async {
    val reactiveCommands = await(reactiveCommandsF)
    new EventSubscriptionImpl(reactiveCommands, callback, eventKeys)
  }

  override def createSubscription(subscriberActor: ActorRef[Event], eventKeys: EventKey*): Future[EventSubscription] = async {
    val reactive = await(reactiveCommandsF)
    new EventSubscriptionImpl(reactive, subscriberActor ! _, eventKeys)
  }
}
