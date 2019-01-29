package csw.alarm.client

import java.time.Duration
import java.util.concurrent.{CompletableFuture, TimeUnit}
import java.util.function.BiFunction

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.alarm.api.javadsl.IAlarmService
import csw.alarm.api.models.AlarmSeverity
import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.internal.auto_refresh.AlarmRefreshActor

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.FiniteDuration

object AlarmRefreshActorFactory {

  /**
   * Scala API - factory to create [[AlarmRefreshActor]]
   *
   * @param alarmService instance of alarm service or custom implementation of [[AlarmService]], you can use lambda expression here
   * @param refreshInterval interval after which alarm will be refreshed
   * @param actorSystem actorSystem used for creating actor
   * @return [[ActorRef]] which accepts [[AutoRefreshSeverityMessage]]
   */
  def make(
      alarmService: AlarmService,
      refreshInterval: FiniteDuration
  )(implicit actorSystem: ActorSystem): ActorRef[AutoRefreshSeverityMessage] =
    actorSystem.spawnAnonymous(
      Behaviors.withTimers[AutoRefreshSeverityMessage](AlarmRefreshActor.behavior(_, alarmService, refreshInterval))
    )

  /**
   * Java API - factory to create [[AlarmRefreshActor]]
   *
   * @param alarmService instance of alarm service or custom implementation of [[AlarmService]], you can use lambda expression here
   * @param refreshInterval interval after which alarm will be refreshed
   * @param actorSystem actorSystem used for creating actor
   * @return [[ActorRef]] which accepts [[AutoRefreshSeverityMessage]]
   */
  def jMake(
      alarmService: IAlarmService,
      refreshInterval: Duration,
      actorSystem: ActorSystem
  ): ActorRef[AutoRefreshSeverityMessage] =
    make(alarmService.asScala, FiniteDuration(refreshInterval.toNanos, TimeUnit.NANOSECONDS))(actorSystem)

  /**
   * Java API - factory to create [[AlarmRefreshActor]]
   *
   * @param setSeverity function responsible for setting severity of alarm
   * @param refreshInterval interval after which alarm will be refreshed
   * @param actorSystem actorSystem used for creating actor
   * @return [[ActorRef]] which accepts [[AutoRefreshSeverityMessage]]
   */
  def jMake(
      setSeverity: BiFunction[AlarmKey, AlarmSeverity, CompletableFuture[Done]],
      refreshInterval: Duration,
      actorSystem: ActorSystem
  ): ActorRef[AutoRefreshSeverityMessage] =
    make(
      (key: AlarmKey, severity: AlarmSeverity) ⇒ setSeverity(key, severity).toScala,
      FiniteDuration(refreshInterval.toNanos, TimeUnit.NANOSECONDS)
    )(actorSystem)
}
