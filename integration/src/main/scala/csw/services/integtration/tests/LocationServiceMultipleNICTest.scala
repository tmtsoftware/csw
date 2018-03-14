package csw.services.integtration.tests

import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{AkkaLocation, ComponentId, ComponentType}
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.CswCluster
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite, Matchers}

class LocationServiceMultipleNICTest(cswCluster: CswCluster)
    extends FunSuite
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll {

  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  override protected def afterAll(): Unit =
    locationService.shutdown(TestFinishedReason)

  test("should list and resolve component having multiple-nic's") {

    val componentId = ComponentId("assembly", ComponentType.Assembly)
    val connection  = AkkaConnection(componentId)

    Thread.sleep(4000)
    val listOfLocations = locationService.list.await

    listOfLocations should have size 1

    val assemblyLocation = locationService.find(connection).await.get

    assemblyLocation shouldBe a[AkkaLocation]

  }

}
