package csw.apps.clusterseed.client

import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.location.DetectComponentRestartTest

class DetectComponentRestartTestWithHttpMultiJvmNode1 extends DetectComponentRestartTestWithHttp(0, "http")
class DetectComponentRestartTestWithHttpMultiJvmNode2 extends DetectComponentRestartTestWithHttp(0, "http")
class DetectComponentRestartTestWithHttpMultiJvmNode3 extends DetectComponentRestartTestWithHttp(0, "http")

// DEOPSCSW-429: [SPIKE] Provide HTTP server and client for location service
class DetectComponentRestartTestWithHttp(ignore: Int, mode: String)
    extends DetectComponentRestartTest(ignore, mode)
    with HTTPLocationService {

  override def afterAll(): Unit = maybeWiring.map(_.actorRuntime.shutdown(TestFinishedReason).await)

}
