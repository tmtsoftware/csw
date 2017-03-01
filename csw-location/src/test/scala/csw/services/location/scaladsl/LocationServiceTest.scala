package csw.services.location.scaladsl

import java.net.URI

import akka.actor.{Actor, ActorPath, Props}
import akka.serialization.Serialization
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.common.{ActorRuntime, Networks}
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

class LocationServiceTest extends FunSuite with Matchers with MockFactory {

  test("tcp integration") {
    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)

    val locationService = LocationService.make(ActorRuntime.make("test"))

    val registrationResult = locationService.register(TcpRegistration(connection, Port)).await

    registrationResult.componentId shouldBe componentId

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(connection, Networks.getPrimaryIpv4Address.getHostAddress, Port)
    )
  }

  test("http integration") {
    val Port = 1234
    val componentId = ComponentId("config-service", ComponentType.Service)
    val connection = HttpConnection(componentId)
    val Path = "path123"

    val locationService = LocationService.make(ActorRuntime.make("test"))

    val registrationResult = locationService.register(HttpRegistration(connection, Port, Path)).await

    registrationResult.componentId shouldBe componentId

    val uri = new URI(s"http://${Networks.getPrimaryIpv4Address.getHostName}:$Port/$Path")

    locationService.list.await shouldBe List(
      ResolvedHttpLocation(connection, uri, Path)
    )
  }


  test("akka integration") {
    val Port = 1234
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    val Prefix = "prefix"
    val actorSystem = ActorRuntime.make("test")

    val locationService = LocationService.make(actorSystem)
    val actorRef = actorSystem.actorOf(
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
  }
}
