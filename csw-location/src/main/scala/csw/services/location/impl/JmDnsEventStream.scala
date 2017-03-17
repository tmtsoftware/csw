package csw.services.location.impl

import javax.jmdns.{JmDNS, ServiceEvent, ServiceListener}

import akka.NotUsed
import akka.stream.scaladsl.Source
import csw.services.location.impl.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.models.{Connection, Location, Removed, Unresolved}
import csw.services.location.scaladsl.ActorRuntime

class JmDnsEventStream(jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private val (_source, queueF) = SourceExtensions.coupling[Location]

  val locationStream: Source[Location, NotUsed] = _source

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
