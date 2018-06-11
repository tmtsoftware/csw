package csw.services.event.internal.redis

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.internal.commons.javawrappers.JEventService
import csw.services.event.javadsl.IEventService
import csw.services.event.scaladsl.EventService
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object RedisEventServiceFactory {
  val masterId = "eventServer"

  def make(locationService: LocationService)(
      implicit actorSystem: ActorSystem[_]
  ): Future[EventService] = {
    implicit lazy val ec: ExecutionContext = actorSystem.executionContext
    async {
      val eventServiceResolver = new EventServiceResolver(locationService)
      val uri                  = await(eventServiceResolver.uri)
      make(uri.getHost, uri.getPort)
    }
  }

  def make(host: String, port: Int)(
      implicit actorSystem: ActorSystem[_]
  ): EventService = {

    implicit lazy val system: actor.ActorSystem = actorSystem.toUntyped
    implicit lazy val ec: ExecutionContext      = system.dispatcher
    val settings                                = ActorMaterializerSettings(system).withSupervisionStrategy(Supervision.getResumingDecider)
    implicit val resumingMat: ActorMaterializer = ActorMaterializer(settings)

    new RedisEventService(host, port, masterId, RedisClient.create())
  }

  def jMake(
      locationService: ILocationService,
      actorSystem: ActorSystem[_]
  ): Future[IEventService] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    val eventServiceF                 = make(locationService.asScala)(actorSystem)
    eventServiceF.map(new JEventService(_))
  }

  def jMake(
      host: String,
      port: Int,
      actorSystem: ActorSystem[_]
  ): IEventService = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    val eventService                  = make(host, port)(actorSystem)
    new JEventService(eventService)
  }
}
