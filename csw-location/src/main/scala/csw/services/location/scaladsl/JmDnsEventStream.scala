package csw.services.location.scaladsl

import javax.jmdns.{JmDNS, ServiceEvent, ServiceListener}

import akka.stream.scaladsl.{BroadcastHub, Keep, Source}
import akka.stream.{KillSwitch, KillSwitches}
import csw.services.location.common.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.common.{ActorRuntime, StreamExtensions}
import csw.services.location.models.{Connection, Location, Removed, Unresolved}

class JmDnsEventStream(jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  jmDns.addServiceListener(LocationService.DnsType, makeListener())

  private val (_source, queueF) = StreamExtensions.coupling[Location]

  lazy val source: Source[Location, KillSwitch] = _source
    .runWith(BroadcastHub.sink[Location])
    .viaMat(KillSwitches.single)(Keep.right)

  private def makeListener() = new ServiceListener {
    override def serviceAdded(event: ServiceEvent) = {
      val connection = Connection.parse(event.getName).get
      queueF.foreach { queue =>
        queue.offer(Unresolved(connection))
      }
    }

    override def serviceResolved(event: ServiceEvent) = {
      val connection = Connection.parse(event.getName).get
      queueF.foreach { queue =>
        event.getInfo.locations.foreach(location => queue.offer(location))
      }
    }

    override def serviceRemoved(event: ServiceEvent) = {
      val connection = Connection.parse(event.getName).get
      queueF.foreach { queue =>
        queue.offer(Removed(connection))
      }
    }
  }
}
