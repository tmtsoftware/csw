package csw.services.integtration.tests

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService
import org.scalatest._

class LocationServiceIntegrationTest(locationService: LocationService)
  extends FunSuite
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll {

  test("resolves remote HCD") {
    val componentId = ComponentId("trombonehcd", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val hcdLocation = locationService.resolve(connection).await.get

    hcdLocation shouldBe a[AkkaLocation]
    hcdLocation
      .asInstanceOf[AkkaLocation]
      .uri
      .toString should not be empty
  }

  test("list all components"){
    val listOfLocations = locationService.list.await

    listOfLocations should have size 2
  }

  test("resolves remote Service") {

    val componentId = ComponentId("redisservice", ComponentType.Service)
    val connection = HttpConnection(componentId)

    val hcdLocation = locationService.resolve(connection).await.get

    hcdLocation shouldBe a[HttpLocation]
    hcdLocation
      .asInstanceOf[HttpLocation]
      .uri
      .toString should not be empty
  }

//  TODO: TestApp does not get terminated when this test enabled.
/*
  test("Registration should validate unique name of service"){
    val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
    val componentId = ComponentId("trombonehcd", ComponentType.HCD)
    val connection = AkkaConnection(componentId)

    val registration = AkkaRegistration(connection, tromboneHcdActorRef, "nfiraos.ncc.tromboneHCD")
    val illegalStateException = intercept[IllegalStateException]{
      locationService.register(registration).await
    }

    illegalStateException.getMessage shouldBe (s"A service with name ${registration.connection.name} is already registered")
  }
*/
}
