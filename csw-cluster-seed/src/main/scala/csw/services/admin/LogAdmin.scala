package csw.services.admin

import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType}
import csw.services.location.scaladsl.LocationService
import csw.services.logging.internal.LoggingLevels.{INFO, Level}

import scala.async.Async._
import scala.concurrent.Future

class LogAdmin(locationService: LocationService) {

  import scala.concurrent.ExecutionContext.Implicits.global
  def getLogLevel(componentName: String): Future[Unit] = async {

    val component = getAkkaConnection(componentName) match {
      case Some(connection) ⇒ await(locationService.find(connection))
      case _                ⇒ throw new IllegalArgumentException(s"$componentName is not valid component name")
    }

    component match {
      case Some(location) ⇒ location.connection
      case None           ⇒ throw new RuntimeException
    }
  }

  def setLogLevel(componentName: String, logLevel: Level): Boolean =
    true

  def getAkkaConnection(componentName: String): Option[AkkaConnection] = {
    val ComponentNamePattern = "([0-9a-zA-Z._]+)-([a-zA-Z]+)-(*)".r
    componentName match {
      case ComponentNamePattern(component, componentType, connection) ⇒
        Some(AkkaConnection(ComponentId(component, ComponentType.withName(componentType))))
      case _ ⇒ None
    }
  }
}
