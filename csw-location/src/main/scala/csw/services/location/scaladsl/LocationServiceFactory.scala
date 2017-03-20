package csw.services.location.scaladsl

import javax.jmdns.JmDNS

import csw.services.location.internal.{LocationEventStream, JmDnsEventStream, LocationServiceImpl, Networks}

object LocationServiceFactory {
  def make(actorRuntime: ActorRuntime): LocationService = {
    val jmDNS = JmDNS.create(actorRuntime.ipaddr)
    val jmDnsEventStream = new JmDnsEventStream(jmDNS, actorRuntime)
    val locationEventStream = new LocationEventStream(jmDnsEventStream, jmDNS, actorRuntime)
    new LocationServiceImpl(jmDNS, actorRuntime, locationEventStream)
  }
}
