package csw.services.location.scaladsl

import csw.messages.location.Connection.TcpConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.messages.models.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.location.commons.{ClusterSettings, CswCluster, RegistrationFactory}
import csw.services.location.internal.Networks
import csw.services.location.models.TcpRegistration
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

class MultiActorSystemTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val connection: TcpConnection        = TcpConnection(ComponentId("exampleTCPService", ComponentType.Service))
  val tcpRegistration: TcpRegistration = RegistrationFactory.tcp(connection, 1234)

  private val locationService =
    LocationServiceFactory.withCluster(CswCluster.withSettings(ClusterSettings().onPort(3552)))
  private val locationService2 =
    LocationServiceFactory.withCluster(CswCluster.withSettings(ClusterSettings().joinLocal(3552)))

  override protected def afterAll(): Unit =
    locationService2.shutdown(TestFinishedReason).await

  test("ensure that location service works across two actorSystems within the same JVM") {
    locationService.register(tcpRegistration).await
    locationService2.resolve(connection, 5.seconds).await.get shouldBe tcpRegistration.location(
      new Networks().hostname()
    )

    locationService.shutdown(TestFinishedReason).await
    locationService2.resolve(connection, 5.seconds).await.get shouldBe tcpRegistration.location(
      new Networks().hostname()
    )
  }
}
