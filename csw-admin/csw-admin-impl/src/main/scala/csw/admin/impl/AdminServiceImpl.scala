package csw.admin.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.{Askable, _}
import akka.util.Timeout
import csw.admin.api.{AdminService, UnresolvedAkkaLocationException}
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
    val prefix         = componentId.prefix

    locationService
      .find(akkaConnection)
      .flatMap(mayBeAkkaLocation =>
        mayBeAkkaLocation
          .map(akkaLocation => {
            log.info(
              "Getting log information from logging system",
              Map("prefix" -> prefix.toString, "location" -> akkaLocation.toString)
            )
            val response: Future[LogMetadata] = componentId.componentType match {
              case Sequencer => akkaLocation.sequencerRef ? (GetComponentLogMetadata(prefix, _))
              case _         => akkaLocation.componentRef ? (GetComponentLogMetadata(prefix, _))
            }
            response
          })
          .getOrElse[Future[LogMetadata]](throw new UnresolvedAkkaLocationException(prefix))
      )
  }

  override def setLogLevel(componentId: ComponentId, level: Level): Future[Unit] = {
    val akkaConnection = AkkaConnection(componentId)
    val prefix         = componentId.prefix

    locationService
      .find(akkaConnection)
      .map(mayBeAkkaLocation =>
        mayBeAkkaLocation
          .map(akkaLocation => {
            log.info(
              s"Setting log level to $level",
              Map("prefix" -> prefix.toString, "location" -> akkaLocation.toString)
            )
            componentId.componentType match {
              case Sequencer => akkaLocation.sequencerRef ! SetComponentLogLevel(prefix, level)
              case _         => akkaLocation.componentRef ! SetComponentLogLevel(prefix, level)
            }
          })
          .getOrElse[Unit](throw new UnresolvedAkkaLocationException(prefix))
      )
  }
}
