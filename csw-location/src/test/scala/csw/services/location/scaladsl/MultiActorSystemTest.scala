package csw.services.location.scaladsl

import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, TcpRegistration}
import csw.services.logging.utils.CswTestSuite

import scala.concurrent.duration.DurationInt

class MultiActorSystemTest extends CswTestSuite {

  val connection: TcpConnection        = TcpConnection(ComponentId("exampleTCPService", ComponentType.Service))
  val tcpRegistration: TcpRegistration = TcpRegistration(connection, 1234)

  private val locationService =
    LocationServiceFactory.withCluster(CswCluster.withSettings(ClusterSettings().onPort(3552)))
  private val locationService2 =
    LocationServiceFactory.withCluster(CswCluster.withSettings(ClusterSettings().joinLocal(3552)))

  override protected def afterAllTests(): Unit =
    locationService2.shutdown().await

  test("ensure that location service works across two actorSystems within the same JVM") {
    locationService.register(tcpRegistration).await
    locationService2.resolve(connection, 5.seconds).await.get shouldBe tcpRegistration.location(
        new Networks().hostname())

    locationService.shutdown().await
    locationService2.resolve(connection, 5.seconds).await.get shouldBe tcpRegistration.location(
        new Networks().hostname())
  }
}
