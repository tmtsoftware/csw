package csw.services.location.impl

import javax.jmdns.{JmDNS, ServiceEvent, ServiceListener}

import csw.services.location.scaladsl.ActorRuntime
import csw.services.location.common.{Constants, SourceExtensions}
import csw.services.location.models.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.models.{Connection, Location, Removed, Unresolved}

class JmDnsEventStream(jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private val (source, queueF) = SourceExtensions.coupling[Location]

  val broadcast: LocationBroadcast = new LocationBroadcast(source, actorRuntime)

  jmDns.addServiceListener(Constants.DnsType, makeListener())

  private def makeListener() = new ServiceListener {
    override def serviceAdded(event: ServiceEvent): Unit = {
      queueF.foreach { queue =>
        Connection.parse(event.getName).map(conn => queue.offer(Unresolved(conn)))
      }
    }

    override def serviceResolved(event: ServiceEvent): Unit = {
      queueF.foreach { queue =>
        event.getInfo.locations.foreach(location => queue.offer(location))
      }
    }

    override def serviceRemoved(event: ServiceEvent): Unit = {
      queueF.foreach { queue =>
        Connection.parse(event.getName).map(conn => queue.offer(Removed(conn)))
      }
    }
  }
}
