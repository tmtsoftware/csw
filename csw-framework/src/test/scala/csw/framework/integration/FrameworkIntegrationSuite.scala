package csw.framework.integration
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.FrameworkTestWiring
import csw.location.impl.internal.{ServerWiring, Settings}
import org.mockito.MockitoSugar
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

trait FrameworkIntegrationSuite
    extends FunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues
    with MockitoSugar {

  private val locationWiring = new ServerWiring(Settings("csw-location-server"))
  val testWiring             = new FrameworkTestWiring()

  override protected def beforeAll(): Unit = locationWiring.locationHttpService.start()

  override protected def afterAll(): Unit = {
    testWiring.shutdown()
    locationWiring.actorRuntime.shutdown().await
  }

}
