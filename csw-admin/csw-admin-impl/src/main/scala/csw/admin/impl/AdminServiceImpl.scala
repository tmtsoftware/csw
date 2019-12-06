package csw.admin.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.{Askable, _}
import akka.util.Timeout
import csw.admin.api.AdminService
import csw.admin.api.AdminServiceError.UnresolvedAkkaLocation
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.ComponentType._
import csw.location.models.Connection.AkkaConnection
import csw.logging.api.scaladsl.Logger
import csw.logging.models.{Level, LogMetadata}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class AdminServiceImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AdminService {
  import actorSystem.executionContext
  private val log: Logger       = AdminLogger.getLogger
  implicit val timeout: Timeout = Timeout(5.seconds)

  override def getLogMetadata(componentId: ComponentId): Future[LogMetadata] = {
    val akkaConnection = AkkaConnection(componentId)
    val componentName  = componentId.prefix.toString

    locationService
      .find(akkaConnection)
      .flatMap(mayBeAkkaLocation =>
        mayBeAkkaLocation
          .map(akkaLocation => {
            log.info(
              "Getting log information from logging system",
              Map("componentName" -> componentName, "location" -> akkaLocation.toString)
            )
            val response: Future[LogMetadata] = componentId.componentType match {
              case Sequencer => akkaLocation.sequencerRef ? (GetComponentLogMetadata(componentName, _))
              case _         => akkaLocation.componentRef ? (GetComponentLogMetadata(componentName, _))
            }
            response
          })
          .getOrElse[Future[LogMetadata]](throw UnresolvedAkkaLocation(componentName))
      )
  }

  override def setLogLevel(componentId: ComponentId, level: Level): Future[Unit] = {
    val akkaConnection = AkkaConnection(componentId)
    val componentName  = componentId.prefix.toString

    locationService
      .find(akkaConnection)
      .map(mayBeAkkaLocation =>
        mayBeAkkaLocation
          .map(akkaLocation => {
            log.info(
              s"Setting log level to $level",
              Map("componentName" -> componentName, "location" -> akkaLocation.toString)
            )
            componentId.componentType match {
              case Sequencer => akkaLocation.sequencerRef ! SetComponentLogLevel(componentName, level)
              case _         => akkaLocation.componentRef ! SetComponentLogLevel(componentName, level)
            }
          })
          .getOrElse[Unit](throw UnresolvedAkkaLocation(componentName))
      )
  }
}
