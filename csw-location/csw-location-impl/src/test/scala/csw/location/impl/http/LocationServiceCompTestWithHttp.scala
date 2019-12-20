package csw.location.impl.http

import csw.location.impl.internal.{ServerWiring, Settings}
import csw.location.impl.commons.TestFutureExtension.RichFuture
import csw.location.impl.scaladsl.LocationServiceCompTest

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {
  private var wiring: ServerWiring = _

  override protected def beforeAll(): Unit = {
    wiring = new ServerWiring(Settings("csw-location-server"))
    wiring.locationHttpService.start().await
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    wiring.actorRuntime.shutdown().await
    super.afterAll()
  }
}
