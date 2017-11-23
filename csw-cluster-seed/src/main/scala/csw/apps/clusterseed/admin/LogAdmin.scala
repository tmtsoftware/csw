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
import csw.services.logging.scaladsl.Logger

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class LogAdmin(locationService: LocationService, actorRuntime: ActorRuntime) {
  val log: Logger = ClusterSeedLogger.getLogger

  import actorRuntime._

  def getLogMetadata(componentFullName: String): Future[LogMetadata] = async {
    implicit val timeout: Timeout = Timeout(5.seconds)
    await(getLocation(componentFullName)) match {
      case Some(location) ⇒
        val logAdminActor = location.logAdminActorRef.upcast[LogControlMessages]
        val componentName = location.connection.componentId.name
        log.info(
          "Getting log information from logging system",
          Map(
            "componentName" → componentFullName,
            "logAdminActor" → logAdminActor.toString,
            "location"      → location.toString
          )
        )
        await(logAdminActor ? (GetComponentLogMetadata(componentName, _)))
      case _ ⇒ throw UnresolvedAkkaOrHttpLocationException(componentFullName)
    }
  }

  def setLogLevel(componentFullName: String, logLevel: Level): Future[Unit] =
    async {
      await(getLocation(componentFullName)) match {
        case Some(location) ⇒
          val logAdminActor = location.logAdminActorRef.upcast[LogControlMessages]
          val componentName = location.connection.componentId.name
          log.info(
            s"Setting log level to $logLevel",
            Map(
              "componentName" → componentFullName,
              "logAdminActor" → logAdminActor.toString,
              "location"      → location.toString
            )
          )
          logAdminActor ! SetComponentLogLevel(componentName, logLevel)
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
}
