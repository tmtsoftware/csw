package csw.event.client.internal.redis

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.EventSubscription
import csw.params.events.Event
import io.lettuce.core.{RedisClient, RedisURI}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RomaineFactory
import romaine.reactive.{RedisSubscription, RedisSubscriptionApi}

import scala.concurrent.Future

class RedisGlobalSubscriber(redisURI: Future[RedisURI], redisClient: RedisClient)(implicit actorSystem: ActorSystem[_]) {
  import EventRomaineCodecs._
  import actorSystem.executionContext

  private lazy val romaineFactory   = new RomaineFactory(redisClient)
  private val globalSubscriptionKey = "*.*.*"

  def subscribeAll(): Source[Event, EventSubscription] = {
    val redisSubscriptionApi: RedisSubscriptionApi[String, Event] = romaineFactory.redisSubscriptionApi(redisURI)

    redisSubscriptionApi
      .psubscribe(List(globalSubscriptionKey), OverflowStrategy.LATEST) // todo: think about overflow
      .map(_.value)
      .mapMaterializedValue(subscription)
  }

  private def subscription(rs: RedisSubscription) =
    new EventSubscription {
      override def unsubscribe(): Future[Done] = rs.unsubscribe()
      override def ready(): Future[Done]       = rs.ready()
    }
}

object RedisGlobalSubscriber {
  def make(
      redisClient: RedisClient,
      redisURI: Future[RedisURI]
  )(implicit system: ActorSystem[_]): RedisGlobalSubscriber = new RedisGlobalSubscriber(redisURI, redisClient)
}
