package csw.services.location.scaladsl

import javax.jmdns.JmDNS

import csw.services.location.common.Networks
import csw.services.location.impl.{JmDnsEventStream, LocationServiceImpl}

object LocationServiceFactory {
  def make(actorRuntime: ActorRuntime): LocationService = {
    val jmDNS = JmDNS.create(Networks.getPrimaryIpv4Address)
    val jmDnsEventStream = new JmDnsEventStream(jmDNS, actorRuntime)
    new LocationServiceImpl(jmDNS, actorRuntime, jmDnsEventStream)
  }
}
