package csw.services.event.internal.redis

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import csw.services.event.internal.commons.EventServiceFactory
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.EventService
import io.lettuce.core.RedisClient

import scala.concurrent.ExecutionContext

class RedisEventServiceFactory(redisClient: RedisClient = RedisClient.create()) extends EventServiceFactory {
  private lazy val masterId: String = ConfigFactory.load().getString("redis.masterId")

  protected override def eventServiceImpl(eventServiceResolver: EventServiceResolver)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext,
      mat: Materializer
  ): EventService =
    new RedisEventService(eventServiceResolver, masterId, redisClient)
}
