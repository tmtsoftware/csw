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
      println(s"EVENT ADDED $event")
    }

    override def serviceResolved(event: ServiceEvent) = {

      println("Validating that we are able to resolve HCD registred in separate container")

      val listOfLocations = locationService.list.await
      val hcdLocation: Location = listOfLocations(0)

      listOfLocations should not be empty
      listOfLocations should have size 1
      hcdLocation shouldBe a[ResolvedAkkaLocation]
      hcdLocation
        .asInstanceOf[ResolvedAkkaLocation]
        .uri
        .toString should not be empty
    }

    override def serviceRemoved(event: ServiceEvent) = {
    }
  }

  test("resolves remote HCD") {
    jmDNS.addServiceListener("_csw._tcp.local.", listner)
    Thread.sleep(10000)
  }
}
