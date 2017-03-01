package csw.services.location.scaladsl

import java.net.{NetworkInterface, URI}
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

    val jmDNS: JmDNS = JmDNS.create(NetworkInterface.getByName("eth0").getInetAddresses().nextElement());

    val actorSystem = ActorSystem("location-service")

    val locationService = LocationService.make(jmDNS, actorSystem)


    val registrationResult = locationService.register(TcpRegistration(connection, Port)).await

    jmDNS.getServiceInfo(LocationService.DnsType, connection.toString) should be ""

    jmDNS.list(LocationService.DnsType) should be ""

    registrationResult.componentId shouldBe componentId

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(connection, NetworkInterface.getByName("eth0").getInetAddresses().nextElement().toString, Port)
    )
  }

/*
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
  */

}
