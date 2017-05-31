package csw.apps.clusterseed.admin

import akka.pattern.ask
import akka.util.Timeout
import csw.apps.clusterseed.admin.exceptions.UnresolvedAkkaLocationException
import csw.apps.clusterseed.admin.internal.ActorRuntime
import csw.services.location.models.{AkkaLocation, Connection}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.internal.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.services.logging.models.LogMetadata

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class LogAdmin(locationService: LocationService, actorRuntime: ActorRuntime) {

  import actorRuntime._

  def getLogMetadata(componentName: String): Future[LogMetadata] = async {
    implicit val timeout = Timeout(5.seconds)
    await(getLocation(componentName)) match {
      case Some(AkkaLocation(_, _, actorRef)) ⇒
        await((actorRef ? GetComponentLogMetadata).mapTo[LogMetadata])
      case _ ⇒ throw UnresolvedAkkaLocationException(componentName)
    }
  }

  def setLogLevel(componentName: String, logLevel: Level): Future[Unit] = async {

    await(getLocation(componentName)) match {
      case Some(AkkaLocation(_, _, actorRef)) ⇒ actorRef ! SetComponentLogLevel(logLevel)
      case _                                  ⇒ throw UnresolvedAkkaLocationException(componentName)
    }
  }

  private def getLocation(componentName: String) = async {
    Connection.from(componentName) match {
      case connection: Connection ⇒ await(locationService.find(connection))
      case _                      ⇒ throw new IllegalArgumentException(s"$componentName is not a valid component name")
    }
  }
}
