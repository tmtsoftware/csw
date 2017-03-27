package csw.services.location.scaladsl

import csw.services.location.internal.Settings
import org.scalatest.{FunSuite, Matchers}

class LocationServiceFactoryTest
  extends FunSuite
    with Matchers {

  /*test("able to create location service using parameterless make") {
    //#Location-service-creation
    val locationService = LocationServiceFactory.make()
    //#Location-service-creation
    locationService.isInstanceOf[LocationService] shouldBe true
  }*/

  test("able to create location service by providing actor runtime") {
    //#Location-service-creation-using-actor-runtime
    val actorRuntime = new ActorRuntime()
    val locationService= LocationServiceFactory.make(actorRuntime)
    //#Location-service-creation-using-actor-runtime
    actorRuntime.terminate()
  }
}
