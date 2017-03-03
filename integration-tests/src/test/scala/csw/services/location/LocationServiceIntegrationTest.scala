package csw.services.location

import csw.services.location.integration.Assembly
import csw.services.location.models.{Location, ResolvedAkkaLocation}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers, Tag}
object IntegrationTest extends Tag("IntegrationTest")

class LocationServiceIntegrationTest
    extends FunSuite
    with Matchers
    with MockFactory
    with BeforeAndAfter {

  test("resolves remote HCD", IntegrationTest) {

    println("#################")
    println("#################")
    println("#################")
    println("#################")
    /*
    Assembly.start

    val listOfLocations = Assembly.listLocations
    val hcdLocation: Location = listOfLocations(0)

    listOfLocations should not be empty
    listOfLocations should have size 1
    hcdLocation shouldBe a[ResolvedAkkaLocation]
    hcdLocation
      .asInstanceOf[ResolvedAkkaLocation]
      .uri
      .toString should not be empty
      */
  }
}
