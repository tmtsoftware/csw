package csw.clusterseed.client

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.clusterseed.internal.AdminWiring
import csw.location.commons.TestFutureExtension.RichFuture
import csw.location.scaladsl.LocationServiceCompTest

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {

  private val wiring = new AdminWiring
  wiring.locationHttpService.start().await

  override protected def afterAll(): Unit = {
    wiring.actorRuntime.shutdown(UnknownReason).await
    super.afterAll()
  }
}
