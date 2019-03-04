package csw.alarm.api.internal
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models.{FullAlarmSeverity, Key}
import csw.alarm.api.scaladsl.{AlarmService, AlarmSubscription}

import scala.concurrent.Future

private[alarm] trait SeverityService extends AlarmService {

  /**
   * Fetches the severity of the given alarm from the alarm store
   *
   * @param alarmKey represents a unique alarm in alarm store
   * @return a future which completes with the alarm severity or fails with [[csw.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getCurrentSeverity(alarmKey: AlarmKey): Future[FullAlarmSeverity]

  /**
   * Gets the aggregated severity which is calculated based on the worst severity amongst all active alarms for the given
   * alarm/component/subsystem/system
   *
   * @param key represents an alarm, component, subsystem or system
   * @return a future which completes with aggregated severity or fails with
   *         [[csw.alarm.api.exceptions.KeyNotFoundException]]
   *         or [[csw.alarm.api.exceptions.InactiveAlarmException]]
   */
  def getAggregatedSeverity(key: Key): Future[FullAlarmSeverity]

  /**
   * Calculates the aggregated severity for the given alarm/component/subsystem/system and executes the callback each time the
   * aggregation changes
   *
   * @note Callbacks are not thread-safe on the JVM. If you need to do side effects/mutations, prefer using [[subscribeAggregatedSeverityActorRef]] API.
   *
   * @note aggregated severity is worst amongst all active alarms for given key
   * @param key represents an alarm, component, subsystem or system
   * @param callback executed with the latest worst severity
   * @return alarm subscription which can be used to unsubscribe or check if subscription is ready to be consumed. The method
   *         can also throw [[csw.alarm.api.exceptions.KeyNotFoundException]] or
   *         [[csw.alarm.api.exceptions.InactiveAlarmException]]
   */
  def subscribeAggregatedSeverityCallback(key: Key, callback: FullAlarmSeverity ⇒ Unit): AlarmSubscription

  /**
   * Calculates the aggregated severity for the given alarm/component/subsystem/system and sends to the give actor each time the
   * aggregation changes
   *
   * @note aggregated severity is worst amongst all active alarms for given key
   * @param key represents an alarm, component, subsystem or system
   * @param actorRef receives the latest worst severity
   * @return alarm subscription which can be used to unsubscribe or check if subscription is ready to be consumed. The method
   *         can also throw [[csw.alarm.api.exceptions.KeyNotFoundException]] or
   *         [[csw.alarm.api.exceptions.InactiveAlarmException]]
   */
  def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[FullAlarmSeverity]): AlarmSubscription

  private[alarm] def subscribeAggregatedSeverity(key: Key): Source[FullAlarmSeverity, AlarmSubscription]
}
