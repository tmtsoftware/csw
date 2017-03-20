package csw.services.location.internal

import akka.pattern.ask
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.internal.DeathwatchActor.{GetLiveAkkaConnections, LiveAkkaConnections}
import csw.services.location.internal.wrappers.JmDnsApi
import csw.services.location.models.{Connection, Location, Removed, ResolvedAkkaLocation}
import csw.services.location.scaladsl.ActorRuntime

import scala.async.Async._
import scala.concurrent.Future

class LocationEventStream(locationStream: Source[Location, KillSwitch], jmDnsApi: JmDnsApi, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private val (queue, removedStream) = StreamExt.coupling[Removed]

  val broadcast: LocationBroadcast = {
    val completeStream = locationStream.merge(removedStream, eagerComplete = true)
    new LocationBroadcast(completeStream, actorRuntime)
  }

  private val actorRefFuture = async {
    actorSystem.actorOf(
      DeathwatchActor.props(await(jmDnsList), locationStream, queue)
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
    jmDnsApi.list(Constants.DnsType)
  }(jmDnsDispatcher)

}
