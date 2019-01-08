package csw.framework.integration
import akka.actor.CoordinatedShutdown.UnknownReason
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.FrameworkTestWiring
import csw.location.server.internal.ServerWiring
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.mockito.MockitoSugar

trait FrameworkIntegrationSuite
    extends FunSuite
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
    locationWiring.actorRuntime.shutdown(UnknownReason).await
  }

}
