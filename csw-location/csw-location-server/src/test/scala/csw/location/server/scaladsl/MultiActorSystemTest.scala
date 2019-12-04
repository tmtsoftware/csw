package csw.location.server.scaladsl

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.TcpConnection
import csw.location.models.{ComponentId, ComponentType, TcpRegistration}
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.commons._
import csw.location.server.internal.LocationServiceFactory
import csw.prefix.{Prefix, Subsystem}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

class MultiActorSystemTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  var connection: TcpConnection = _

  private var system1: ActorSystem[SpawnProtocol.Command] = _
  private var system2: ActorSystem[SpawnProtocol.Command] = _

  private var locationService: LocationService  = _
  private var locationService2: LocationService = _

  var tcpRegistration: TcpRegistration = _

  override protected def beforeAll(): Unit = {
    connection = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "exampleTCPService"), ComponentType.Service))
    system1 = ClusterSettings().onPort(3558).system
    system2 = ClusterSettings().joinLocal(3558).system
    locationService = LocationServiceFactory.withCluster(CswCluster.withSystem(system1))
    locationService2 = LocationServiceFactory.withCluster(CswCluster.withSystem(system2))
    tcpRegistration = TcpRegistration(connection, 1234)
  }

  override protected def afterAll(): Unit = {
    system2.terminate()
    system2.whenTerminated.await
  }

  test("ensure that location service works across two actorSystems within the same JVM") {
    locationService.register(tcpRegistration).await
    locationService2.resolve(connection, 5.seconds).await.get.connection shouldBe tcpRegistration.connection

    system1.terminate()
    system1.whenTerminated.await
    locationService2.resolve(connection, 5.seconds).await.get.connection shouldBe tcpRegistration.connection
  }
}
