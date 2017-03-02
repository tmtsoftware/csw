package csw.services.location.scaladsl

import javax.jmdns.{JmDNS, ServiceEvent, ServiceListener}

import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import akka.stream.{KillSwitch, KillSwitches, OverflowStrategy}
import csw.services.location.common.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.common.{ActorRuntime, StreamExtensions}
import csw.services.location.models.{Connection, Location, Removed, Unresolved}

class JmDnsEventStream(jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  jmDns.addServiceListener(LocationService.DnsType, makeListener())

  private val (_source, queueF) = StreamExtensions.coupling[Location]

  val source: Source[Location, KillSwitch] = _source
    .runWith(BroadcastHub.sink[Location])
    .viaMat(KillSwitches.single)(Keep.right)
    .buffer(256, OverflowStrategy.dropNew)

  // Ensure that the Broadcast output is dropped if there are no listening parties.
  source.runWith(Sink.ignore)

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
