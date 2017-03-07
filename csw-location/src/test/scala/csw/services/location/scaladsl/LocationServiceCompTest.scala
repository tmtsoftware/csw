package csw.services.location.scaladsl

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.common.{ActorRuntime, Networks}
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalatest._

class LocationServiceCompTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val actorRuntime = new ActorRuntime("test")
  private val locationService = LocationServiceFactory.make(actorRuntime)

  override protected def afterEach(): Unit = {
    locationService.unregisterAll().await
  }

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }

  test("tcp location") {

    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)

    val registrationResult = locationService.register(TcpRegistration(connection, Port)).await

    registrationResult.componentId shouldBe componentId

    val uri = new URI(s"tcp://${Networks.getPrimaryIpv4Address.getHostAddress}:$Port")

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(connection, uri)
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }

  test("http location") {
    val Port = 1234
    val componentId = ComponentId("configService", ComponentType.Service)
    val connection = HttpConnection(componentId)
    val Path = "path123"

    val registrationResult = locationService.register(HttpRegistration(connection, Port, Path)).await

    registrationResult.componentId shouldBe componentId

    val uri = new URI(s"http://${Networks.getPrimaryIpv4Address.getHostAddress}:$Port/$Path")

    locationService.list.await shouldBe List(
      ResolvedHttpLocation(connection, uri, Path)
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }

  test("akka location") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val Prefix = "prefix"

    val actorRef = actorRuntime.actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = Actor.emptyBehavior
      }),
      "my-actor-1"
    )

    val registrationResult = locationService.register(AkkaRegistration(connection, actorRef, Prefix)).await

    registrationResult.componentId shouldBe componentId

    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(actorRef))
    val uri = new URI(actorPath.toString)

    locationService.list.await shouldBe List(
      ResolvedAkkaLocation(connection, uri, Prefix, Some(actorRef))
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }

  test("tracking") {

    val reg = TcpRegistration(TcpConnection(ComponentId("redis2", ComponentType.Service)), 1234)
    val reg2 = TcpRegistration(TcpConnection(ComponentId("redis3", ComponentType.Service)), 1111)

    import actorRuntime._

    val source = locationService.track(reg.connection)

    val (switch, probe) = source.toMat(TestSink.probe[Location])(Keep.both).run()

    val registrationResult2 = locationService.register(reg2).await
    registrationResult2.unregister().await

    val registrationResult = locationService.register(reg).await
    registrationResult.unregister().await

    val uri = new URI(s"tcp://${Networks.getPrimaryIpv4Address.getHostAddress}:${reg.port}")

    probe
      .request(3)
      .expectNext(Unresolved(reg.connection))
      .expectNext(ResolvedTcpLocation(reg.connection, uri))
      .expectNext(Removed(reg.connection))

    switch.shutdown()

    probe
      .request(1)
      .expectComplete()

  }
}
