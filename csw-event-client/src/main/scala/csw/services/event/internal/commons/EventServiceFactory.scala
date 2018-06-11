package csw.services.event.internal.commons
import java.util.concurrent.CompletableFuture

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.commons.javawrappers.JEventService
import csw.services.event.javadsl.IEventService
import csw.services.event.scaladsl.EventService
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}

abstract class EventServiceFactory {
  protected def eventServiceImpl(host: String, port: Int)(
      implicit actorSystem: actor.ActorSystem,
      ec: ExecutionContext,
      mat: Materializer
  ): EventService

  def make(locationService: LocationService)(implicit actorSystem: ActorSystem[_]): Future[EventService] = {
    implicit lazy val ec: ExecutionContext = actorSystem.executionContext
    async {
      val eventServiceResolver = new EventServiceResolver(locationService)
      val uri                  = await(eventServiceResolver.uri)
      make(uri.getHost, uri.getPort)
    }
  }

  def make(host: String, port: Int)(implicit actorSystem: ActorSystem[_]): EventService = {
    implicit lazy val system: actor.ActorSystem = actorSystem.toUntyped
    implicit lazy val ec: ExecutionContext      = system.dispatcher
    val settings                                = ActorMaterializerSettings(system).withSupervisionStrategy(Supervision.getResumingDecider)
    implicit val resumingMat: ActorMaterializer = ActorMaterializer(settings)

    eventServiceImpl(host, port)
  }

  def jMake(locationService: ILocationService, actorSystem: ActorSystem[_]): CompletableFuture[IEventService] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    val eventServiceF                 = make(locationService.asScala)(actorSystem)
    eventServiceF.map[IEventService](new JEventService(_)).toJava.toCompletableFuture
  }

  def jMake(host: String, port: Int, actorSystem: ActorSystem[_]): IEventService = {
    val eventService = make(host, port)(actorSystem)
    new JEventService(eventService)
  }
}
