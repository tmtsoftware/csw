package csw.alarm.api.internal
import akka.actor.typed.ActorRef
import csw.alarm.api.models.{AlarmHealth, Key}
import csw.alarm.api.scaladsl.AlarmSubscription

import scala.concurrent.Future

private[alarm] trait HealthService {

  /**
   * Gets the aggregated health which is calculated based on the worst severity amongst all active alarms for the given
   * alarm/component/subsystem/system
   *
   * @param key represents an alarm, component, subsystem or system
   * @return a future which completes with aggregated health or fails with
   *         [[csw.alarm.api.exceptions.KeyNotFoundException]]
   *         or [[csw.alarm.api.exceptions.InactiveAlarmException]]
   */
  def getAggregatedHealth(key: Key): Future[AlarmHealth]

  /**
   * Calculates the aggregated health for the given alarm/component/subsystem/system and executes the callback each time the
   * aggregation changes
   *
   * @note Callbacks are not thread-safe on the JVM. If you need to do side effects/mutations, prefer using [[subscribeAggregatedHealthActorRef]] API.
   *
   * @note aggregated health is worst amongst all active alarms for given key
   * @param key represents an alarm, component, subsystem or system
   * @param callback executed with the latest worst health
   * @return alarm subscription which can be used to unsubscribe or check if subscription is ready to be consumed. The method
   *         can also throw [[csw.alarm.api.exceptions.KeyNotFoundException]] or
   *         [[csw.alarm.api.exceptions.InactiveAlarmException]]
   */
  def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription

  /**
   * Calculates the aggregated health for the given alarm/component/subsystem/system and sends to the give actor each time the
   * aggregation changes
   *
   * @note aggregated health is worst amongst all active alarms for given key
   * @param key represents an alarm, component, subsystem or system
   * @param actorRef receives the latest worst health
   * @return alarm subscription which can be used to unsubscribe or check if subscription is ready to be consumed. The method
   *         can also throw [[csw.alarm.api.exceptions.KeyNotFoundException]] or
   *         [[csw.alarm.api.exceptions.InactiveAlarmException]]
   */
  def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription
}
