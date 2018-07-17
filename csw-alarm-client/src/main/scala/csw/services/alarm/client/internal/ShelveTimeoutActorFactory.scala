package csw.services.alarm.client.internal

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.services.alarm.api.models.AlarmKey

import scala.concurrent.Future

class ShelveTimeoutActorFactory {
  def make(unshelve: AlarmKey ⇒ Future[Unit])(implicit actorSystem: ActorSystem): ActorRef[AlarmTimeoutMessage] =
    actorSystem.spawnAnonymous {
      Behaviors
        .withTimers[AlarmTimeoutMessage] { timerScheduler ⇒
          Behaviors
            .setup[AlarmTimeoutMessage] { ctx ⇒
              new ShelveTimeoutBehaviour(ctx, timerScheduler, unshelve)
            }
        }
    }
}
