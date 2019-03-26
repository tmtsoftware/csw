package csw.location.server.http

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.internal.ServerWiring
import csw.location.server.scaladsl.LocationServiceCompTest

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {
  private var wiring: ServerWiring = _

  override protected def beforeAll(): Unit = {
    wiring = new ServerWiring
    wiring.locationHttpService.start().await
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    wiring.actorRuntime.shutdown(UnknownReason).await
    super.afterAll()
  }
}
