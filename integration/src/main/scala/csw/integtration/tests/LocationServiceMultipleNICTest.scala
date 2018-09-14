package csw.integtration.tests

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.commons.CswCluster
import csw.location.scaladsl.LocationServiceFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite, Matchers}

class LocationServiceMultipleNICTest(cswCluster: CswCluster)
    extends FunSuite
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll
    with Eventually {

  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  implicit val patience: PatienceConfig =
    PatienceConfig(Span(5, org.scalatest.time.Seconds), Span(100, org.scalatest.time.Millis))

  override protected def afterAll(): Unit =
    locationService.shutdown(UnknownReason)

  test("should list and resolve component having multiple-nic's") {

    val componentId = ComponentId("assembly", ComponentType.Assembly)
    val connection  = AkkaConnection(componentId)

    eventually(locationService.list.await should have size 1)

    locationService.find(connection).await.get shouldBe a[AkkaLocation]
  }

}
