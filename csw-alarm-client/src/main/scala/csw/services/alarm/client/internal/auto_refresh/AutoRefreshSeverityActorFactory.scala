package csw.services.alarm.client.internal.auto_refresh

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps

class AutoRefreshSeverityActorFactory {
  def make(alarm: Refreshable)(implicit actorSystem: ActorSystem): ActorRef[AutoRefreshSeverityMessage] =
    actorSystem.spawnAnonymous {
      Behaviors
        .withTimers[AutoRefreshSeverityMessage] { timerScheduler ⇒
          Behaviors
            .setup[AutoRefreshSeverityMessage] { ctx ⇒
              new AutoRefreshSeverityBehavior(ctx, timerScheduler, alarm)
            }
        }
    }
}
