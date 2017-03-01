package csw.services.location.scaladsl

import java.net.URI
import javax.jmdns.JmDNS

import akka.actor.ActorSystem
import csw.services.location.common.Networks
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.{HttpConnection, TcpConnection}
import csw.services.location.models._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

class LocationServiceTest extends FunSuite with Matchers with MockFactory {

  test("tcp integration") {
    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val connection = TcpConnection(componentId)

    val locationService = LocationService.make()

    val registrationResult = locationService.register(TcpRegistration(connection, Port)).await

    registrationResult.componentId shouldBe componentId

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(connection, Networks.getPrimaryIpv4Address.getHostName, Port)
    )
  }

  test("http integration") {
    val Port = 1234
    val componentId = ComponentId("config", ComponentType.Service)
    val connection = HttpConnection(componentId)
    val Path = "path123"

    val jmDNS: JmDNS = JmDNS.create()

    val actorSystem = ActorSystem("test")
    val locationService = LocationService.make(jmDNS, actorSystem)

    val registrationResult = locationService.register(HttpRegistration(connection, Port, Path)).await

    registrationResult.componentId shouldBe componentId

    val uri = new URI(s"http://${Networks.getPrimaryIpv4Address.getHostName}:$Port/$Path")

    locationService.list.await shouldBe List(
      ResolvedHttpLocation(connection, uri, Path)
    )
  }
}
