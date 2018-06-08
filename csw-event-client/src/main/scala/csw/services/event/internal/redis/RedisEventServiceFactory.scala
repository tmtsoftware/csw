package csw.services.event.internal.redis

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.internal.commons.javawrappers.JEventService
import csw.services.event.javadsl.IEventService
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient

import scala.concurrent.ExecutionContext

object RedisEventServiceFactory {

  def make(locationService: LocationService, redisClient: RedisClient)(
      implicit actorSystem: ActorSystem[_]
  ): RedisEventService = {

    implicit lazy val system: actor.ActorSystem = actorSystem.toUntyped
    implicit lazy val ec: ExecutionContext      = system.dispatcher
    lazy val settings =
      ActorMaterializerSettings(system).withSupervisionStrategy(Supervision.getResumingDecider)

    implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)

    val masterId             = "eventServer"
    val eventServiceResolver = new EventServiceResolver(locationService)

    new RedisEventService(redisClient, masterId, eventServiceResolver)
  }

  def jMake(locationService: ILocationService, redisClient: RedisClient, actorSystem: ActorSystem[_]): IEventService = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    new JEventService(make(locationService.asScala, redisClient)(actorSystem))
  }
}
