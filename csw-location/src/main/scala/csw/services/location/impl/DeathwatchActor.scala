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
    currentLocations.collect {
      case ResolvedAkkaLocation(connection, _, _, Some(actorRef)) => watchIt(actorRef, connection)
    }
    stream.collect {
      case ResolvedAkkaLocation(connection, _, _, Some(actorRef)) => self ! WatchIt(actorRef, connection)
    }.runWith(Sink.ignore)
  }

  override def receive: Receive = {
    case WatchIt(actorRef, connection) =>
      watchIt(actorRef, connection)
    case Terminated(deadActorRef)      =>
      watchedAkkaLocations.get(deadActorRef).foreach { connection =>
        queue.offer(Removed(connection))
      }
      watchedAkkaLocations -= deadActorRef
    case GetLiveAkkaConnections        =>
      sender() ! LiveAkkaConnections(watchedAkkaLocations.values.toSet)
  }

  def watchIt(actorRef: ActorRef, connection: Connection): Unit = {
    watchedAkkaLocations += actorRef -> connection
    context.watch(actorRef)
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
