package csw.alarm.client.internal.auto_refresh

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.alarm.api.scaladsl.AlarmService

import scala.concurrent.duration.FiniteDuration

class AutoRefreshSeverityActorFactory {
  def make(alarmService: AlarmService,
           refreshInterval: FiniteDuration)(implicit actorSystem: ActorSystem): ActorRef[AutoRefreshSeverityMessage] =
    actorSystem.spawnAnonymous(
      Behaviors.withTimers[AutoRefreshSeverityMessage](AutoRefreshSeverityActor.behavior(_, alarmService, refreshInterval))
    )
}
