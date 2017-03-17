package csw.services.location.impl

import javax.jmdns.JmDNS

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import csw.services.location.impl.ServiceInfoExtensions.RichServiceInfo
import csw.services.location.models.{Connection, Location, Removed, ResolvedAkkaLocation}
import csw.services.location.scaladsl.ActorRuntime
import akka.pattern.ask
import csw.services.location.impl.DeathwatchActor.{GetLiveAkkaConnections, LiveAkkaConnections}
import csw.services.location.impl.SourceExtensions.RichSource

import async.Async._
import scala.concurrent.Future

class LocationEventStream(jmDnsEventStream: JmDnsEventStream, jmDns: JmDNS, actorRuntime: ActorRuntime) {

  import actorRuntime._

  @volatile
  private var actorRefOpt: Option[ActorRef] = None

  private val (removedStream, queueF) = SourceExtensions.coupling[Removed]

  private val stream = jmDnsEventStream.locationStream.broadcast()

  val broadcast: LocationBroadcast = {
    val completeStream = stream.merge(removedStream)
    new LocationBroadcast(completeStream, actorRuntime)
  }

  queueF.foreach { queue =>
    val currentLocations = Source.fromFuture(jmDnsList).mapConcat(identity)
    val allLocations = currentLocations.concat(stream)
    actorRefOpt = Some(actorSystem.actorOf(DeathwatchActor.props(allLocations, queue)))
  }

  def list: Future[List[Location]] = actorRefOpt
    .map(via)
    .getOrElse(jmDnsList)

  private def via(actorRef: ActorRef): Future[List[Location]] = async {
    val eventualConnections = (actorRef ? GetLiveAkkaConnections).mapTo[LiveAkkaConnections]
    val liveAkkaConnections = await(eventualConnections).connections
    await(filter(liveAkkaConnections))
  }

  private def filter(liveAkkaConnections: Set[Connection]) = async {
    await(jmDnsList).filter {
      case x: ResolvedAkkaLocation => liveAkkaConnections.contains(x.connection)
      case _                       => true
    }
  }

  def jmDnsList: Future[List[Location]] = Future {
    jmDns.list(Constants.DnsType).toList.flatMap(_.locations)
  }(jmDnsDispatcher)

}
