package csw.services.event.internal

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.pubsub.{EventPublisherImpl, EventSubscriberImpl}
import csw.services.event.internal.redis.{
  RedisEventPublisherDriver,
  RedisEventSubscriberDriver,
  RedisEventSubscriberDriverFactory
}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.ExecutionContext

class Wiring(redisPort: Int) {
  lazy val redisURI: RedisURI       = RedisURI.create("localhost", redisPort)
  lazy val redisClient: RedisClient = RedisClient.create(redisURI)

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher
  implicit lazy val mat: Materializer        = ActorMaterializer()

  lazy val publisherDriver            = new RedisEventPublisherDriver(redisClient, redisURI)
  lazy val subscriberDriver           = new RedisEventSubscriberDriver(redisClient, redisURI)
  private val subscriberDriverFactory = new RedisEventSubscriberDriverFactory(redisClient, redisURI)

  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  lazy val resumingMat: Materializer = ActorMaterializer(settings)

  lazy val publisherImpl  = new EventPublisherImpl(publisherDriver)(ec, resumingMat)
  lazy val subscriberImpl = new EventSubscriberImpl(subscriberDriverFactory)(ec, resumingMat)
}
