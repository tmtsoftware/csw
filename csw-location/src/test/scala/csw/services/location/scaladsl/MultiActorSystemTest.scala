package csw.services.location.scaladsl

import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, TcpRegistration}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class MultiActorSystemTest extends FunSuite with Matchers with BeforeAndAfterAll {

  val connection: TcpConnection = TcpConnection(ComponentId("exampleTCPService", ComponentType.Service))
  val tcpRegistration: TcpRegistration = TcpRegistration(connection, 1234)

  private val locationService = LocationServiceFactory.withCluster(CswCluster.withSettings(ClusterSettings().onPort(3552)) )
  private val locationService2 = LocationServiceFactory.withCluster(CswCluster.withSettings(ClusterSettings().joinLocal(3552)))

  override protected def afterAll(): Unit = {
    locationService2.shutdown().await
  }

  test("ensure that location service works across two actorSystems within the same JVM") {
    locationService.register(tcpRegistration).await
    Thread.sleep(2000)
    locationService2.resolve(connection).await.get shouldBe tcpRegistration.location(new Networks().hostname())

    locationService.shutdown().await
    locationService2.resolve(connection).await.get shouldBe tcpRegistration.location(new Networks().hostname())
  }
}
