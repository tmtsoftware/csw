package csw.config.client

import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.config.server.mocks.MockedAuthentication
import csw.location.impl.internal.{ServerWiring, Settings}
import org.mockito.MockitoSugar
import org.scalatest._

trait ConfigClientBaseSuite
    extends MockedAuthentication
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar {
  private val locationWiring = new ServerWiring(Settings("csw-location-server"))

  override protected def beforeAll(): Unit = locationWiring.locationHttpService.start()

  override protected def afterAll(): Unit = locationWiring.actorRuntime.shutdown().await

}
