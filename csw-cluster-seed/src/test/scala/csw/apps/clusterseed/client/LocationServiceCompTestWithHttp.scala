package csw.apps.clusterseed.client

import akka.http.scaladsl.Http
import csw.apps.clusterseed.internal.AdminWiring
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.LocationServiceCompTest

import scala.util.control.NonFatal

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {

  private val wiring = new AdminWiring

  val binding: Http.ServerBinding = wiring.locationHttpService.start().await

  override protected def afterAll(): Unit = {
    super.afterAll()
    binding.unbind().await
    Http(wiring.actorSystem).shutdownAllConnectionPools().recover { case NonFatal(_) ⇒ /* ignore */ }.await
    wiring.actorRuntime.shutdown(TestFinishedReason).await
  }
}
