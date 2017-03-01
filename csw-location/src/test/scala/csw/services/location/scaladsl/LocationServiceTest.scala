package csw.services.location.scaladsl

import javax.jmdns.JmDNS

import akka.actor.ActorSystem
import csw.services.location.common.Networks
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, ResolvedTcpLocation, TcpRegistration}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

class LocationServiceTest extends FunSuite with Matchers with MockFactory {

  test("integration") {
    val Port = 1234
    val componentId = ComponentId("redis1", ComponentType.Service)
    val tcpConnection = TcpConnection(componentId)

    val jmDNS: JmDNS = JmDNS.create()

    val actorSystem = ActorSystem("test")
    val locationService = LocationService.make(jmDNS, actorSystem)

    val registrationResult = locationService.register(TcpRegistration(tcpConnection, Port)).await

    registrationResult.componentId shouldBe componentId

    locationService.list.await shouldBe List(
      ResolvedTcpLocation(tcpConnection, Networks.getPrimaryIpv4Address.getHostName, Port)
    )
  }

}
