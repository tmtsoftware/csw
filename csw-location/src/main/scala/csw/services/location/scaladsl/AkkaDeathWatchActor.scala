package csw.services.location.scaladsl

import akka.actor.{Actor, ActorRef, Props, Terminated}
import csw.services.location.impl.JmDnsEventStream
import csw.services.location.models.Connection

case class WatchIt(val actorRef: ActorRef, connection: Connection)
case class UnWatchIt(val actorRef: ActorRef)

class DeathwatchActor(val stream:JmDnsEventStream) extends Actor {
  private var watchedAkkaLocations = Map[ActorRef, Connection]()

  override def receive: Receive = {
    case w@WatchIt(actorRef, connection) => {
      watchedAkkaLocations += actorRef -> connection
      context.watch(w.actorRef)
    }
    case Terminated(deadActorRef) => {
      val connectionMayBe: Option[Connection] = watchedAkkaLocations.get(deadActorRef)
      connectionMayBe.map(c=>stream.actorTerminated(c))
    }
  }
}

object DeathwatchActor {
  def props(stream: JmDnsEventStream) = Props(new DeathwatchActor(stream))
}