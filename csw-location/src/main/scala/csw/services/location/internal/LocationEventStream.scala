package csw.services.location.internal

import javax.jmdns.JmDNS

import akka.pattern.ask
import csw.services.location.internal.DeathwatchActor.{GetLiveAkkaConnections, LiveAkkaConnections}
import csw.services.location.internal.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.models.{Connection, Location, Removed, ResolvedAkkaLocation}
import csw.services.location.scaladsl.ActorRuntime

import scala.async.Async._
import scala.concurrent.Future

class LocationEventStream(jmDnsEventStream: JmDnsEventStream, jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private val (queue, removedStream) = StreamExt.coupling[Removed]

  private val stream = jmDnsEventStream.locationStream

  val broadcast: LocationBroadcast = {
    val completeStream = stream.merge(removedStream, eagerComplete = true)
    new LocationBroadcast(completeStream, actorRuntime)
  }

  private val actorRefFuture = async {
    actorSystem.actorOf(
      DeathwatchActor.props(await(jmDnsList), stream, queue)
    )
  }

  def list: Future[List[Location]] = async {
    val liveAkkaConnectionsF = (await(actorRefFuture) ? GetLiveAkkaConnections).mapTo[LiveAkkaConnections]
    val connections = await(liveAkkaConnectionsF).connections
    await(filter(connections))
  }

  private def filter(liveAkkaConnections: Set[Connection]) = async {
    await(jmDnsList).filter {
      case x: ResolvedAkkaLocation => liveAkkaConnections.contains(x.connection)
      case _                       => true
    }
  }

  private def jmDnsList: Future[List[Location]] = Future {
    jmDns.list(Constants.DnsType).toList.flatMap(_.locations)
  }(jmDnsDispatcher)

}
