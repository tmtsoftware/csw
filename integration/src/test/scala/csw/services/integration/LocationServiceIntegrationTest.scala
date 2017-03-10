package csw.services.integration

import csw.services.location.common.ActorRuntime
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

class LocationServiceIntegrationTest
  extends FunSuite
    with Matchers
    with MockFactory
    with BeforeAndAfter {

  private val actorRuntime = new ActorRuntime("AssemblySystem")
  private val locationService = LocationServiceFactory.make(actorRuntime)

  test("resolves remote HCD") {
    val componentId = ComponentId("trombonehcd", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val hcdLocation = locationService.resolve(connection).await

    hcdLocation shouldBe a[ResolvedAkkaLocation]
    hcdLocation
          .asInstanceOf[ResolvedAkkaLocation]
          .uri
          .toString should not be empty
  }

  test("resolve list of all locations"){
    val listOfLocations = locationService.list.await

    listOfLocations should not be empty
    listOfLocations should have size 2
  }
}
