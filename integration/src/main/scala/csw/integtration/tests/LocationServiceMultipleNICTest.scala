package csw.integtration.tests

import csw.integtration.common.TestFutureExtension.RichFuture
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterAwareSettings
import csw.location.server.internal.ServerWiring
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class LocationServiceMultipleNICTest()
    extends AnyFunSuite
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  implicit val patience: PatienceConfig =
    PatienceConfig(Span(5, org.scalatest.time.Seconds), Span(100, org.scalatest.time.Millis))

  val adminWiring: ServerWiring = ServerWiring.make(ClusterAwareSettings.onPort(3553).withInterface("eth1"), enableAuth = false)
  LoggingSystemFactory.start("Assembly", "1.0", Networks().hostname, adminWiring.actorSystem)

  adminWiring.locationHttpService.start().futureValue

  import adminWiring.actorRuntime._
  private val locationService = HttpLocationServiceFactory.makeLocalClient

  override def afterAll(): Unit = Await.result(adminWiring.actorRuntime.shutdown(), 5.seconds)

  test("should list and resolve component having multiple-nic's") {

    val componentId = ComponentId(Prefix(Subsystem.CSW, "assembly"), ComponentType.Assembly)
    val connection  = AkkaConnection(componentId)

    eventually(locationService.list.await should have size 1)

    locationService.find(connection).await.get shouldBe a[AkkaLocation]
  }

}
