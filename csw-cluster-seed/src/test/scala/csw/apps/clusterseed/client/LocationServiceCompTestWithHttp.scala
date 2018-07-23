package csw.apps.clusterseed.client

import akka.http.scaladsl.Http
import csw.apps.clusterseed.internal.AdminWiring
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.LocationServiceCompTest

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {

  private val wiring = new AdminWiring

  val binding: Http.ServerBinding = wiring.locationHttpService.start().await

  override protected def afterAll(): Unit = {
    super.afterAll()
    wiring.actorSystem.terminate().await
  }
}
