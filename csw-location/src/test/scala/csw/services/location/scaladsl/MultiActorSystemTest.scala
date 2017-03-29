package csw.services.location.scaladsl

import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.internal.{Networks, Settings}
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, TcpRegistration}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class MultiActorSystemTest extends FunSuite with Matchers with BeforeAndAfterAll {

  val connection: TcpConnection = TcpConnection(ComponentId("exampleTCPService", ComponentType.Service))
  val tcpRegistration: TcpRegistration = TcpRegistration(connection, 1234)

  val actorRuntime = new ActorRuntime(Settings().withPort(2552))
  val actorRuntime2 = new ActorRuntime(Settings().withPort(2553))

  val locationService = LocationServiceFactory.make(actorRuntime)
  val locationService2 = LocationServiceFactory.make(actorRuntime2)

  override protected def afterAll(): Unit = {
    locationService2.shutdown().await
  }

  test("ensure that location service works across two actorSystems within the same JVM") {
    locationService.register(tcpRegistration).await
    Thread.sleep(500)
    locationService2.resolve(connection).await.get shouldBe tcpRegistration.location(new Networks().hostname())

    locationService.shutdown().await
    locationService2.resolve(connection).await.get shouldBe tcpRegistration.location(new Networks().hostname())
  }
}
