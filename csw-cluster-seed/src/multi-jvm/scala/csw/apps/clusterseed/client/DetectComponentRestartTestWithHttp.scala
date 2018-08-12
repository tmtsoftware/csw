package csw.apps.clusterseed.client

import akka.http.scaladsl.Http
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.location.DetectComponentRestartTest

import scala.util.control.NonFatal

class DetectComponentRestartTestWithHttpMultiJvmNode1 extends DetectComponentRestartTestWithHttp(0, "http")
class DetectComponentRestartTestWithHttpMultiJvmNode2 extends DetectComponentRestartTestWithHttp(0, "http")
class DetectComponentRestartTestWithHttpMultiJvmNode3 extends DetectComponentRestartTestWithHttp(0, "http")

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class DetectComponentRestartTestWithHttp(ignore: Int, mode: String)
    extends DetectComponentRestartTest(ignore, mode)
    with HTTPLocationService {

  override def afterAll(): Unit = {
    maybeBinding.map(_.unbind().await)
    maybeWiring.map { wiring ⇒
      Http(wiring.actorSystem)
        .shutdownAllConnectionPools()
        .recover { case NonFatal(_) ⇒ /* ignore */ }(testSystem.dispatcher)
        .await
      wiring.actorRuntime.shutdown(TestFinishedReason).await
    }
    testSystem.terminate().await
  }

}
