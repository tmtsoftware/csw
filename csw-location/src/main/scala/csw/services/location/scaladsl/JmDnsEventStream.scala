package csw.services.location.scaladsl

import javax.jmdns.{JmDNS, ServiceEvent, ServiceListener}

import csw.services.location.common.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.common.{ActorRuntime, SourceExtensions}
import csw.services.location.models.{Connection, Location, Removed, Unresolved}

class JmDnsEventStream(jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private val (source, queueF) = SourceExtensions.coupling[Location]

  val broadcast: LocationBroadcast = new LocationBroadcast(source, actorRuntime)

  jmDns.addServiceListener(LocationService.DnsType, makeListener())

  private def makeListener() = new ServiceListener {
    override def serviceAdded(event: ServiceEvent): Unit = {
      queueF.foreach { queue =>
        queue.offer(Unresolved(Connection.parse(event.getName).get))
      }
    }

    override def serviceResolved(event: ServiceEvent): Unit = {
      queueF.foreach { queue =>
        event.getInfo.locations.foreach(location => queue.offer(location))
      }
    }

    override def serviceRemoved(event: ServiceEvent): Unit = {
      queueF.foreach { queue =>
        queue.offer(Removed(Connection.parse(event.getName).get))
      }
    }
  }
}
