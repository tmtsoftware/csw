package csw.services.location

import csw.services.location.integration.AssemblyApp
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType, Location, ResolvedAkkaLocation}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}


class LocationServiceIntegrationTest
  extends FunSuite
    with Matchers
    with MockFactory
    with BeforeAndAfter {

  test("resolves remote HCD") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val Prefix = "prefix"

    AssemblyApp.start
    AssemblyApp.listLocations should not be empty
    val hcdLocation:Location = AssemblyApp.listLocations(0)
    hcdLocation match {
      case r:ResolvedAkkaLocation => {
        r.uri.toString should not be empty
      }
      case _ => fail("Could not resolve HCD actor reference")
    }
  }
}
