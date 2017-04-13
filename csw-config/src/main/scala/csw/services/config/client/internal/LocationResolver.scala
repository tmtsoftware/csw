package csw.services.config.client.internal

import csw.services.location.models.Connection.HttpConnection
import csw.services.location.models.{ComponentId, ComponentType, Location}
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


class LocationResolver(locationService: LocationService) {

  def configServiceLocation: Location = {
    val connection = HttpConnection(ComponentId("ConfigServiceServer", ComponentType.Service))

    // FIXME: fix resolve method in such a way that within a given time, wait for connection to get available rather than depending upon list call
    // this sleep requires as a workaround so that data get replicated and ConfigClient can
    // resolve HTTP ConfigServer
    Thread.sleep(3000)
    Await.result(locationService.resolve(connection), 5.seconds).get
  }

}
