package csw.services.logging.scaladsl

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.services.logging.internal.{LogAdminActor, LogControlMessages}

object LogAdminActorFactory {
  def make(actorSystem: ActorSystem): ActorRef[LogControlMessages] =
    actorSystem.spawnAnonymous(LogAdminActor.behavior())
}
