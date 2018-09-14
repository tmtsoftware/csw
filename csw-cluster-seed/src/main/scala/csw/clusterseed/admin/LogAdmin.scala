package csw.clusterseed.admin

import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.clusterseed.admin.exceptions.{InvalidComponentNameException, UnresolvedAkkaOrHttpLocationException}
import csw.clusterseed.internal.ActorRuntime
import csw.clusterseed.commons.ClusterSeedLogger
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.scaladsl.LocationService
import csw.location.api.models.{Connection, Location}
import csw.logging.internal.LoggingLevels.Level
import csw.logging.messages.{GetComponentLogMetadata, LogControlMessages, SetComponentLogLevel}
import csw.logging.models.LogMetadata
import csw.logging.scaladsl.Logger

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

/**
 * Utility to resolve and get the actorRef of LogAdminActor for the component using location service
 */
class LogAdmin(locationService: LocationService, actorRuntime: ActorRuntime) {
  private val log: Logger = ClusterSeedLogger.getLogger

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
