package csw.apps.clusterseed.admin

import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.apps.clusterseed.admin.exceptions.{InvalidComponentNameException, UnresolvedAkkaOrHttpLocationException}
import csw.apps.clusterseed.admin.internal.ActorRuntime
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.messages.location.Connection.{AkkaConnection, HttpConnection}
import csw.messages.location.{Connection, Location}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.internal.{GetComponentLogMetadata, LogControlMessages, SetComponentLogLevel}
import csw.services.logging.models.LogMetadata

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class LogAdmin(locationService: LocationService, actorRuntime: ActorRuntime) extends ClusterSeedLogger.Simple {

  import actorRuntime._

  def getLogMetadata(componentFullName: String): Future[LogMetadata] = async {
    implicit val timeout: Timeout = Timeout(5.seconds)
    await(getLocation(componentFullName)) match {
      case Some(location) ⇒
        log.info("Getting log information from logging system", Map("location" → location.toString))
        await(
          typedLogAdminActor(location) ? (GetComponentLogMetadata(componentName(location), _))
        )
      case _ ⇒ throw UnresolvedAkkaOrHttpLocationException(componentFullName)
    }
  }

  def setLogLevel(componentFullName: String, logLevel: Level): Future[Unit] =
    async {
      await(getLocation(componentFullName)) match {
        case Some(location) ⇒
          log.info(s"Setting log level to $logLevel", Map("location" → location.toString))
          typedLogAdminActor(location) ! SetComponentLogLevel(componentName(location), logLevel)
        case _ ⇒ throw UnresolvedAkkaOrHttpLocationException(componentFullName)
      }
    }

  private def getLocation(componentFullName: String): Future[Option[Location]] =
    async {
      Connection.from(componentFullName) match {
        case connection: AkkaConnection ⇒ await(locationService.find(connection))
        case connection: HttpConnection ⇒ await(locationService.find(connection))
        case _                          ⇒ throw InvalidComponentNameException(componentFullName)
      }
    }

  private def typedLogAdminActor(location: Location) = {
    location.logAdminActorRef.upcast[LogControlMessages]
  }

  private def componentName(location: Location) = {
    location.connection.componentId.name
  }
}
