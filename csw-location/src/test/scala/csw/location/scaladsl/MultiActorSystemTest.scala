package csw.location.scaladsl

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.location.api.commons.ClusterSettings
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.TcpRegistration
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.commons.TestFutureExtension.RichFuture
import csw.location.commons._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

class MultiActorSystemTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val connection: TcpConnection = TcpConnection(ComponentId("exampleTCPService", ComponentType.Service))

  private val system1 = ClusterSettings().onPort(3552).system
  private val system2 = ClusterSettings().joinLocal(3552).system

  private val locationService  = LocationServiceFactory.withCluster(CswCluster.withSystem(system1))
  private val locationService2 = LocationServiceFactory.withCluster(CswCluster.withSystem(system2))

  val RegistrationFactory              = new TestRegistrationFactory()(system1)
  val tcpRegistration: TcpRegistration = RegistrationFactory.tcp(connection, 1234)

  override protected def afterAll(): Unit =
    locationService2.shutdown(UnknownReason).await

  test("ensure that location service works across two actorSystems within the same JVM") {
    locationService.register(tcpRegistration).await
    locationService2.resolve(connection, 5.seconds).await.get.connection shouldBe tcpRegistration.connection

    locationService.shutdown(UnknownReason).await
    locationService2.resolve(connection, 5.seconds).await.get.connection shouldBe tcpRegistration.connection
  }
}
