package csw.services.location.scaladsl

import javax.jmdns.{JmDNS, ServiceInfo}

import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, TcpRegistration}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}
import csw.services.location.common.{ActorRuntime, Constants}

import scala.util.Failure

class LocationServiceTest extends FunSuite with Matchers with MockFactory {

  test("future should contain a exception in case jmDNS throws an error") {
    val registration = TcpRegistration(TcpConnection(ComponentId("", ComponentType.HCD)), 100)

    val jmDNS = stub[JmDNS]

    (jmDNS.registerService _).when(registration.serviceInfo).throws(CustomExp)
    (jmDNS.list(_:String)).when(Constants.DnsType).returns(new Array[ServiceInfo](0))

    val runtime = new ActorRuntime("test")
    val locationService: LocationService = new LocationServiceImpl(jmDNS, runtime, null)

    locationService.register(registration).done.value.get shouldBe Failure(CustomExp)
  }

  case object CustomExp extends RuntimeException

}
