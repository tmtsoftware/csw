package csw.services.integtration.tests

import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{AkkaLocation, ComponentId, ComponentType}
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.CswCluster
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite, Matchers}

class LocationServiceMultipleNICTest(cswCluster: CswCluster)
    extends FunSuite
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll with Eventually{

  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  implicit val patience: PatienceConfig = PatienceConfig(Span(5, org.scalatest.time.Seconds), Span(100, org.scalatest.time.Millis))

  override protected def afterAll(): Unit =
    locationService.shutdown(TestFinishedReason)

  test("should list and resolve component having multiple-nic's") {

    val componentId = ComponentId("assembly", ComponentType.Assembly)
    val connection  = AkkaConnection(componentId)

    eventually(locationService.list.await should have size 1)

    locationService.find(connection).await.get shouldBe a[AkkaLocation]
  }

}
