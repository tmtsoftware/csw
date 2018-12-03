package csw.config.client

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.config.server.mocks.MockedAuthentication
import csw.location.server.internal.ServerWiring
import org.scalatest.mockito.MockitoSugar
import org.scalatest._

trait ConfigClientBaseSuite
    extends MockedAuthentication
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar {
  private val locationWiring = new ServerWiring

  override protected def beforeAll(): Unit = locationWiring.locationHttpService.start()

  override protected def afterAll(): Unit = locationWiring.actorRuntime.shutdown(UnknownReason).await

}
