package csw.services.alarm.client.internal.shelve

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps

class ShelveTimeoutActorFactory {
  def make(alarm: UnShelvable)(implicit actorSystem: ActorSystem): ActorRef[ShelveTimeoutMessage] =
    actorSystem.spawnAnonymous {
      Behaviors
        .withTimers[ShelveTimeoutMessage] { timerScheduler ⇒
          Behaviors
            .setup[ShelveTimeoutMessage] { ctx ⇒
              new ShelveTimeoutBehaviour(ctx, timerScheduler, alarm)
            }
        }
    }
}
