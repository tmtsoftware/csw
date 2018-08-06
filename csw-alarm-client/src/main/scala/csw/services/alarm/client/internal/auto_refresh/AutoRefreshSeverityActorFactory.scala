package csw.services.alarm.client.internal.auto_refresh

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps

class AutoRefreshSeverityActorFactory {
  def make(alarm: Refreshable)(implicit actorSystem: ActorSystem): ActorRef[AutoRefreshSeverityMessage] =
    actorSystem.spawnAnonymous(Behaviors.withTimers[AutoRefreshSeverityMessage](AutoRefreshSeverityActor.behavior(_, alarm)))
}
