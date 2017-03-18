package csw.services.integration

import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{ComponentId, ComponentType, ResolvedAkkaLocation}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite, Matchers}

class LocationServiceMultipleNICTest extends FunSuite
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll{
  private val actorRuntime = new ActorRuntime("AssemblySystem", "eth1")

  private val locationService = LocationServiceFactory.make(actorRuntime)

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }

  test("should list and resolve component having multiple-nic's"){

    val componentId = ComponentId("assembly", ComponentType.Assembly)
    val connection = AkkaConnection(componentId)

    val listOfLocations = locationService.list.await

    listOfLocations should not be empty
    listOfLocations should have size 1

    val assemblyLocation = locationService.resolve(connection).await

    assemblyLocation shouldBe a[ResolvedAkkaLocation]
    assemblyLocation
      .asInstanceOf[ResolvedAkkaLocation]
      .uri
      .toString should not be empty
  }

}
