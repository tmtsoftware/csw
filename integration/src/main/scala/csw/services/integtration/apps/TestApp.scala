package csw.services.integtration.apps

import csw.services.integtration.tests.{LocationServiceIntegrationTest, TrackLocationAppIntegrationTest}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

object TestApp {
  import org.scalatest

  def main(args: Array[String]): Unit = {
    val actorRuntime = new ActorRuntime()
    val locationService = LocationServiceFactory.make(actorRuntime)
    scalatest.run(new LocationServiceIntegrationTest(locationService))
    scalatest.run(new TrackLocationAppIntegrationTest(locationService))
    actorRuntime.terminate()
    locationService.shutdown()
  }
}
