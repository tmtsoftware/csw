package csw.services.config.client.internal

import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType, Location}
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


class LocationResolver(locationService: LocationService) {

  def configServiceLocation: Location = {
    val connection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))
    Await.result(locationService.resolve(connection), 5.seconds).get
  }

}
