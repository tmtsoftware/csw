package csw.config.client

import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.config.server.mocks.MockedAuthentication
import csw.location.server.internal.ServerWiring
import org.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

trait ConfigClientBaseSuite
    extends MockedAuthentication
    with AnyFunSuiteLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar {
  private val locationWiring = new ServerWiring

  override protected def beforeAll(): Unit = locationWiring.locationHttpService.start()

  override protected def afterAll(): Unit = locationWiring.actorRuntime.shutdown().await

}
