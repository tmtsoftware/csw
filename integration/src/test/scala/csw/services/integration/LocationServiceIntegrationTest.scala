package csw.services.integration

import akka.actor.Props
import csw.services.integtration.apps.TromboneHCD
import csw.services.location.common.ActorRuntime
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
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

  test("list all components"){
    val listOfLocations = locationService.list.await

    listOfLocations should not be empty
    listOfLocations should have size 2
  }

  test("resolves remote Service") {

    val componentId = ComponentId("redisservice", ComponentType.Service)
    val connection = HttpConnection(componentId)

    val hcdLocation = locationService.resolve(connection).await

    hcdLocation shouldBe a[ResolvedHttpLocation]
    hcdLocation
      .asInstanceOf[ResolvedHttpLocation]
      .uri
      .toString should not be empty
  }

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
}
