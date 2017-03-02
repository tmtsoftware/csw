package csw.services.location.scaladsl

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import akka.stream.scaladsl.{Keep, Sink}
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.common.{ActorRuntime, Networks}
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalamock.scalatest.MockFactory
import org.scalatest._

class LocationServiceTest
  extends FunSuite
    with Matchers
    with MockFactory
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val actorRuntime = new ActorRuntime("test")
  private val locationService = LocationService.make(actorRuntime)

  override protected def afterEach(): Unit = {
    locationService.unregisterAll().await
  }

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }

  test("tcp integration") {

    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)

    val registrationResult = locationService.register(TcpRegistration(connection, Port)).await

    registrationResult.componentId shouldBe componentId

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(connection, Networks.getPrimaryIpv4Address.getHostAddress, Port)
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }

  test("http integration") {
    val Port = 1234
    val componentId = ComponentId("config-service", ComponentType.Service)
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

  test("akka integration") {
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
      ResolvedAkkaLocation(connection, uri, Prefix)
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty
  }

  test("tracking") {

    val Port = 1234
    val componentId = ComponentId("redis2", ComponentType.Service)
    val connection = TcpConnection(componentId)

    import actorRuntime.mat

    val (switch, events) = locationService.track(connection).take(3).toMat(Sink.seq)(Keep.both).run()

    val registrationResult = locationService.register(TcpRegistration(connection, Port)).await

    registrationResult.componentId shouldBe componentId

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(connection, Networks.getPrimaryIpv4Address.getHostAddress, Port)
    )

    registrationResult.unregister().await

    locationService.list.await shouldBe List.empty

    events.await shouldBe List(
      Unresolved(connection),
      ResolvedTcpLocation(connection, Networks.getPrimaryIpv4Address.getHostAddress, Port),
      Removed(connection)
    )

    switch.shutdown()
  }

}
