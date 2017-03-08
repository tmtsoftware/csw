package csw.services.integration

import javax.jmdns.{JmDNS, ServiceEvent, ServiceListener}

import csw.services.location.common.{ActorRuntime, Networks}
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.{Location, ResolvedAkkaLocation}
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}

class LocationServiceIntegrationTest
    extends FunSuite
    with Matchers
    with MockFactory
    with BeforeAndAfter {

  private val actorRuntime = new ActorRuntime("AssemblySystem")
  private val locationService = LocationServiceFactory.make(actorRuntime)
  private val ipv4Address = Networks.getPrimaryIpv4Address
  println(s"Using IP4 address $ipv4Address")

  val jmDNS = JmDNS.create(ipv4Address)

  val listner:ServiceListener = new ServiceListener {
    override def serviceAdded(event: ServiceEvent) = {
      println(s"EVENT ADDED event")
    }

    override def serviceResolved(event: ServiceEvent) = {
      println(s"EVENT RESOLVED $event")
    }

    override def serviceRemoved(event: ServiceEvent) = {
      println(s"EVENT REMOVED $event")
    }
  }

  test("resolves remote HCD") {

    jmDNS.addServiceListener("_csw._tcp.local.", listner)
    Thread.sleep(10000)
  }
}
