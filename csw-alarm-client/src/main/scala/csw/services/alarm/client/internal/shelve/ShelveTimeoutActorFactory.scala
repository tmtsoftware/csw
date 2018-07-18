package csw.services.alarm.client.internal.shelve

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.services.alarm.api.models.AlarmKey

import scala.concurrent.Future

class ShelveTimeoutActorFactory {
  def make(unshelve: AlarmKey ⇒ Future[Unit])(implicit actorSystem: ActorSystem): ActorRef[ShelveTimeoutMessage] =
    actorSystem.spawnAnonymous {
      Behaviors
        .withTimers[ShelveTimeoutMessage] { timerScheduler ⇒
          Behaviors
            .setup[ShelveTimeoutMessage] { ctx ⇒
              new ShelveTimeoutBehaviour(ctx, timerScheduler, unshelve)
            }
        }
    }
}
