package csw.services.location.scaladsl

import javax.jmdns.{JmDNS, ServiceInfo}

import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, TcpRegistration}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import csw.services.location.impl.{Constants, JmDnsEventStream, LocationEventStream, LocationServiceImpl}

import scala.util.Failure

class LocationServiceTest
  extends FunSuite
    with Matchers
    with MockFactory
    with BeforeAndAfterAll {

  val actorRuntime = new ActorRuntime("test")

  override protected def afterAll(): Unit = {
    actorRuntime.actorSystem.terminate().await
  }
  
  test("future should contain a exception in case jmDNS throws an error") {
    val registration = TcpRegistration(TcpConnection(ComponentId("", ComponentType.HCD)), 100)

    val jmDNS = stub[JmDNS]
    val jmDnsEventStream = new JmDnsEventStream(jmDNS, actorRuntime)
    val locationEventStream: LocationEventStream = new LocationEventStream(jmDnsEventStream, jmDNS, actorRuntime)
    (jmDNS.registerService _).when(registration.serviceInfo).throws(CustomExp)
    (jmDNS.list(_:String)).when(Constants.DnsType).returns(new Array[ServiceInfo](0))

    val locationService: LocationService = new LocationServiceImpl(jmDNS, actorRuntime, locationEventStream)

    locationService.register(registration).done.value.get shouldBe Failure(CustomExp)
  }

  case object CustomExp extends RuntimeException

}
