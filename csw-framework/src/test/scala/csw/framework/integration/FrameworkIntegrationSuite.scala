package csw.framework.integration
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.FrameworkTestWiring
import csw.location.server.internal.ServerWiring
import org.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

trait FrameworkIntegrationSuite
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues
    with MockitoSugar {

  private val locationWiring = new ServerWiring
  val testWiring             = new FrameworkTestWiring()

  override protected def beforeAll(): Unit = locationWiring.locationHttpService.start()

  override protected def afterAll(): Unit = {
    testWiring.shutdown()
    locationWiring.actorRuntime.shutdown().await
  }

}
