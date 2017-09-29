package csw.apps.clusterseed.admin

import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.apps.clusterseed.admin.exceptions.{InvalidComponentNameException, UnresolvedAkkaLocationException}
import csw.apps.clusterseed.admin.internal.ActorRuntime
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{AkkaLocation, Connection, Location}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.internal.{GetComponentLogMetadata, LogControlMessages, SetComponentLogLevel}
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.models.LogMetadata

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class LogAdmin(locationService: LocationService, actorRuntime: ActorRuntime) extends ClusterSeedLogger.Simple {

  import actorRuntime._

  def getLogMetadata(componentName: String): Future[LogMetadata] = async {
    implicit val timeout: Timeout = Timeout(5.seconds)
    await(getLocation(componentName)) match {
      case Some(akkaLocation @ AkkaLocation(connection, _, actorRef)) ⇒
        log.info("Getting log information from logging system",
                 Map("componentName" → componentName, "actorRef" → actorRef.toString))
        val logMetadataF: Future[LogMetadata] = akkaLocation
          .typedRef[LogControlMessages] ? (GetComponentLogMetadata(connection.componentId.name, _))
        await(logMetadataF)

      case _ ⇒ throw UnresolvedAkkaLocationException(componentName)
    }
  }

  def setLogLevel(componentName: String, logLevel: Level): Future[Unit] =
    async {
      await(getLocation(componentName)) match {

        case Some(akkaLocation @ AkkaLocation(connection, _, actorRef)) ⇒
          log.info(s"Setting log level to $logLevel",
                   Map("componentName" → componentName, "actorRef" → actorRef.toString))
          akkaLocation.typedRef[LogControlMessages] ! SetComponentLogLevel(connection.componentId.name, logLevel)

        case _ ⇒ throw UnresolvedAkkaLocationException(componentName)
      }
    }

  private def getLocation(componentName: String): Future[Option[Location]] =
    async {
      Connection.from(componentName) match {
        case connection: AkkaConnection ⇒ await(locationService.find(connection))
        case _                          ⇒ throw InvalidComponentNameException(componentName)
      }
    }
}
