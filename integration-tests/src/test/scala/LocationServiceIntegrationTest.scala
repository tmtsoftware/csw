import csw.services.location.integration.AssemblyApp
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}


class LocationServiceIntegrationTest
  extends FunSuite
    with Matchers
    with MockFactory
    with BeforeAndAfter {

  test("resolves remote HCD") {
    AssemblyApp.start
    println(AssemblyApp.listLocations)
  }
}
