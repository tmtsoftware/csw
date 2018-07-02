package csw.services.event.internal.redis

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.services.event.internal.commons.EventServiceFactory
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.EventService
import io.lettuce.core.RedisClient

import scala.concurrent.ExecutionContext

/**
 * Factory to create RedisEventService.
 * @param redisClient An instance of RedisClient provided by lettuce driver. If no instance is provided a default new one is created
 */
class RedisEventServiceFactory(redisClient: RedisClient = RedisClient.create()) extends EventServiceFactory {

  protected override def eventServiceImpl(eventServiceResolver: EventServiceResolver)(
      implicit actorSystem: ActorSystem,
      mat: Materializer
  ): EventService = {

    // Redis Sentinel uses this Id to denote the master instance of Redis Server being monitored
    val masterId                      = actorSystem.settings.config.getString("redis.masterId")
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    new RedisEventService(eventServiceResolver, masterId, redisClient)
  }
}
