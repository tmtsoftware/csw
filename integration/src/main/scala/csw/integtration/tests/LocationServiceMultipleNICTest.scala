package csw.integtration.tests

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.clusterseed.client.HTTPLocationService
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.client.scaladsl.HttpLocationServiceFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span

class LocationServiceMultipleNICTest() extends HTTPLocationService with Eventually {

  implicit private val system: ActorSystem    = ActorSystem()
  implicit private val mat: ActorMaterializer = ActorMaterializer()

  private val locationService = HttpLocationServiceFactory.makeLocalClient

  implicit val patience: PatienceConfig =
    PatienceConfig(Span(5, org.scalatest.time.Seconds), Span(100, org.scalatest.time.Millis))

  override def afterAll(): Unit = super.afterAll()

  test("should list and resolve component having multiple-nic's") {

    val componentId = ComponentId("assembly", ComponentType.Assembly)
    val connection  = AkkaConnection(componentId)

    eventually(locationService.list.await should have size 1)

    locationService.find(connection).await.get shouldBe a[AkkaLocation]
  }

}
