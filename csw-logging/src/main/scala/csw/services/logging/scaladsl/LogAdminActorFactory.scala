package csw.services.logging.scaladsl

import akka.actor.ActorSystem
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.services.logging.internal.{LogAdminActor, LogControlMessages}

//TODO: explain better significance
object LogAdminActorFactory {
  def make(actorSystem: ActorSystem): ActorRef[LogControlMessages] =
    actorSystem.spawnAnonymous(LogAdminActor.behavior())
}
