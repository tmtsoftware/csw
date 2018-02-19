package csw.services.event.internal

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.event.internal.pubsub.{EventPublisherImpl, EventSubscriberImpl}
import csw.services.event.internal.redis.{RedisEventPublisherDriver, RedisEventSubscriberDriver}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.ExecutionContext

class Wiring(redisPort: Int) {
  lazy val redisURI: RedisURI       = RedisURI.create("localhost", redisPort)
  lazy val redisClient: RedisClient = RedisClient.create(redisURI)

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  implicit lazy val mat: Materializer        = ActorMaterializer()
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher

  lazy val subscriberDriver = new RedisEventSubscriberDriver(redisClient, redisURI)
  lazy val publisherDriver  = new RedisEventPublisherDriver(redisClient, redisURI)

  lazy val publisherImpl  = new EventPublisherImpl(publisherDriver)
  lazy val subscriberImpl = new EventSubscriberImpl(subscriberDriver)
}
