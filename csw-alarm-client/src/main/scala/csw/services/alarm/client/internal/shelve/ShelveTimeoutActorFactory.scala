package csw.services.alarm.client.internal.shelve

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps

class ShelveTimeoutActorFactory {
  def make(alarm: Unshelvable, shelveTimeoutHour: Int)(implicit actorSystem: ActorSystem): ActorRef[ShelveTimeoutMessage] =
    actorSystem.spawnAnonymous(
      Behaviors.withTimers[ShelveTimeoutMessage](ShelveTimeoutActor.behavior(_, alarm, shelveTimeoutHour))
    )
}
