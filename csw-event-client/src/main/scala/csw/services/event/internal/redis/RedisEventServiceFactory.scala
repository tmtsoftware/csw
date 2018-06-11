package csw.services.event.internal.redis

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.services.event.internal.commons.EventServiceFactory
import csw.services.event.scaladsl.EventService
import io.lettuce.core.RedisClient

import scala.concurrent.ExecutionContext

object RedisEventServiceFactory extends EventServiceFactory {
  val masterId = "eventServer"
  protected override def eventServiceImpl(host: String, port: Int)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext,
      mat: Materializer
  ): EventService =
    new RedisEventService(host, port, masterId, RedisClient.create())
}
