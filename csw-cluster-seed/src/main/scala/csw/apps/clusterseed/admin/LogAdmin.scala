package csw.apps.clusterseed.admin

import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.apps.clusterseed.admin.exceptions.{InvalidComponentNameException, UnresolvedAkkaOrHttpLocationException}
import csw.apps.clusterseed.admin.internal.ActorRuntime
import csw.apps.clusterseed.commons.ClusterSeedLogger
import csw.messages.location.Connection.{AkkaConnection, HttpConnection}
import csw.messages.location.{AkkaLocation, Connection, HttpLocation, Location}
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
    await(getLocation(componentFullName)) match {
      case Some(AkkaLocation(connection, _, actorRef, logAdminActorRef)) ⇒
        log.info(
          "Getting log information from logging system",
          Map(
            "componentName"    → componentFullName,
            "actorRef"         → actorRef.toString,
            "logAdminActorRef" → logAdminActorRef.toString
          )
        )
        await(getLogMetadata(logAdminActorRef, connection.componentId.name))
      case Some(HttpLocation(connection, uri, logAdminActorRef)) ⇒
        log.info(
          "Getting log information from logging system",
          Map(
            "componentName"    → componentFullName,
            "uri"              → uri.toString,
            "logAdminActorRef" → logAdminActorRef.toString
          )
        )
        await(getLogMetadata(logAdminActorRef, connection.componentId.name))
      case _ ⇒ throw UnresolvedAkkaOrHttpLocationException(componentFullName)
    }
  }

  def setLogLevel(componentFullName: String, logLevel: Level): Future[Unit] =
    async {
      await(getLocation(componentFullName)) match {
        case Some(akkaLocation @ AkkaLocation(connection, _, actorRef, logAdminActorRef)) ⇒
          log.info(
            s"Setting log level to $logLevel",
            Map(
              "componentName"    → componentFullName,
              "actorRef"         → actorRef.toString,
              "logAdminActorRef" → logAdminActorRef.toString
            )
          )
          logAdminActorTyped(logAdminActorRef) ! SetComponentLogLevel(connection.componentId.name, logLevel)
        case Some(HttpLocation(connection, uri, logAdminActorRef)) ⇒
          log.info(
            s"Setting log level to $logLevel",
            Map(
              "componentName"    → componentFullName,
              "uri"              → uri.toString,
              "logAdminActorRef" → logAdminActorRef.toString
            )
          )
          logAdminActorTyped(logAdminActorRef) ! SetComponentLogLevel(connection.componentId.name, logLevel)
        case _ ⇒ throw UnresolvedAkkaOrHttpLocationException(componentFullName)
      }
    }

  private def getLogMetadata(logAdminActorRef: ActorRef[Nothing], componentName: String): Future[LogMetadata] = {
    implicit val timeout: Timeout = Timeout(5.seconds)
    logAdminActorTyped(logAdminActorRef) ? (GetComponentLogMetadata(componentName, _))
  }

  private def getLocation(componentFullName: String): Future[Option[Location]] =
    async {
      Connection.from(componentFullName) match {
        case connection: AkkaConnection ⇒ await(locationService.find(connection))
        case connection: HttpConnection ⇒ await(locationService.find(connection))
        case _                          ⇒ throw InvalidComponentNameException(componentFullName)
      }
    }

  private def logAdminActorTyped(logAdminActorRef: ActorRef[_]) =
    logAdminActorRef.asInstanceOf[ActorRef[LogControlMessages]]
}
