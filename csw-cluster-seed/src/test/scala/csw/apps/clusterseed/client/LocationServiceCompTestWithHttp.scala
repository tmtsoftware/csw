package csw.apps.clusterseed.client

import akka.http.scaladsl.Http
import csw.apps.clusterseed.internal.AdminWiring
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.scaladsl.LocationServiceCompTest
import csw.services.logging.appenders.StdOutAppender
import csw.services.logging.internal.LoggingLevels.DEBUG
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggingSystemFactory

import scala.util.control.NonFatal

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class LocationServiceCompTestWithHttp extends LocationServiceCompTest("http") {

  private val wiring = new AdminWiring

  private val loggingSystem: LoggingSystem = LoggingSystemFactory.start("http", "master", "localhost", wiring.actorSystem)
  loggingSystem.setAppenders(List(StdOutAppender))
  loggingSystem.setDefaultLogLevel(DEBUG)
  loggingSystem.setAkkaLevel(DEBUG)
  loggingSystem.setSlf4jLevel(DEBUG)

  val binding: Http.ServerBinding = wiring.locationHttpService.start().await

  override protected def afterAll(): Unit = {
    super.afterAll()
    binding.unbind().await
    Http(wiring.actorSystem).shutdownAllConnectionPools().recover { case NonFatal(_) ⇒ /* ignore */ }.await
    wiring.actorRuntime.shutdown(TestFinishedReason).await
  }
}
