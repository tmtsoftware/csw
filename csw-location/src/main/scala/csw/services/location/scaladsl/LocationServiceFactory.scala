package csw.services.location.scaladsl

import javax.jmdns.JmDNS

import csw.services.location.impl.{JmDnsEventStream, LocationServiceImpl, Networks}

object LocationServiceFactory {
  def make(actorRuntime: ActorRuntime): LocationService = {
    val jmDNS = JmDNS.create(Networks.getPrimaryIpv4Address)
    val jmDnsEventStream = new JmDnsEventStream(jmDNS, actorRuntime)
    new LocationServiceImpl(jmDNS, actorRuntime, jmDnsEventStream)
  }
}
