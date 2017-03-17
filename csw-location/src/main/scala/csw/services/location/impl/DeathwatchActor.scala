package csw.services.location.impl

import akka.NotUsed
import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import csw.services.location.impl.DeathwatchActor.{GetLiveAkkaConnections, LiveAkkaConnections, WatchIt}
import csw.services.location.models.{Connection, Location, Removed, ResolvedAkkaLocation}

class DeathwatchActor(
  stream: Source[Location, NotUsed],
  queue: SourceQueueWithComplete[Removed]
) extends Actor {

  var watchedAkkaLocations: Map[ActorRef, Connection] = Map.empty
  implicit val mat = ActorMaterializer()

  stream.collect {
    case ResolvedAkkaLocation(connection, _, _, Some(actorRef)) =>
      self ! WatchIt(actorRef, connection)
  }.runWith(Sink.ignore)

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
    stream: Source[Location, NotUsed],
    queue: SourceQueueWithComplete[Removed]
  ): Props = Props(new DeathwatchActor(stream, queue))

  case class WatchIt(actorRef: ActorRef, connection: Connection)
  case object GetLiveAkkaConnections
  case class LiveAkkaConnections(connections: Set[Connection])
}
