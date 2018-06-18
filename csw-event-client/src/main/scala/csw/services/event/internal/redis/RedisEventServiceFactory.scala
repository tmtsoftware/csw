package csw.services.event.internal.redis

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.services.event.internal.commons.EventServiceFactory
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.EventService
import io.lettuce.core.RedisClient

import scala.concurrent.ExecutionContext

class RedisEventServiceFactory(redisClient: RedisClient) extends EventServiceFactory {
  val masterId = "eventServer"
  protected override def eventServiceImpl(eventServiceResolver: EventServiceResolver)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext,
      mat: Materializer
  ): EventService =
    new RedisEventService(eventServiceResolver, masterId, redisClient)
}
