package csw.services.integtration.apps

import csw.services.integtration.tests.LocationServiceIntegrationTest
import csw.services.location.scaladsl.LocationServiceFactory

object TestApp {
  import org.scalatest

  def main(args: Array[String]): Unit = {
    val locationService = LocationServiceFactory.make()
    Thread.sleep(2000)
    scalatest.run(new LocationServiceIntegrationTest(locationService))
    locationService.shutdown()
  }
}
