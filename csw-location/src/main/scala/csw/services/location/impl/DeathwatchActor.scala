package csw.services.location.impl

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, KillSwitch}
import csw.services.location.impl.DeathwatchActor.{GetLiveAkkaConnections, LiveAkkaConnections, WatchIt}
import csw.services.location.models.{Connection, Location, Removed, ResolvedAkkaLocation}

class DeathwatchActor(
  currentLocations: List[Location],
  stream: Source[Location, KillSwitch],
  queue: SourceQueueWithComplete[Removed]
) extends Actor {

  var watchedAkkaLocations: Map[ActorRef, Connection] = Map.empty
  implicit val mat = ActorMaterializer()

  override def preStart(): Unit = {
    val watch: PartialFunction[Location, Unit] = {
      case ResolvedAkkaLocation(connection, _, _, Some(actorRef)) => self ! WatchIt(actorRef, connection)
    }
    currentLocations.collect(watch)
    stream.collect(watch).runWith(Sink.ignore)
  }

  override def receive: Receive = {
    case WatchIt(actorRef, connection) =>
      watchedAkkaLocations += actorRef -> connection
      context.watch(actorRef)
    case Terminated(deadActorRef) =>
      watchedAkkaLocations.get(deadActorRef).foreach { connection =>
        queue.offer(Removed(connection))
      }
      watchedAkkaLocations -= deadActorRef
    case GetLiveAkkaConnections =>
      sender() ! LiveAkkaConnections(watchedAkkaLocations.values.toSet)
  }
}

object DeathwatchActor {
  def props(
    currentLocations: List[Location],
    stream: Source[Location, KillSwitch],
    queue: SourceQueueWithComplete[Removed]
  ): Props = Props(new DeathwatchActor(currentLocations, stream, queue))

  case class WatchIt(actorRef: ActorRef, connection: Connection)
  case object GetLiveAkkaConnections
  case class LiveAkkaConnections(connections: Set[Connection])
}
