package csw.services.alarm.api.internal
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.{FullAlarmSeverity, Key}
import csw.services.alarm.api.scaladsl.{AlarmService, AlarmSubscription}

import scala.concurrent.Future

private[alarm] trait SeverityService extends AlarmService {

  /**
   * To fetch the severity of a specific alarm from the alarm store
   *
   * @param key represents a unique alarm in alarm store
   * @return a future which completes with severity fetched from the alarm store or fails with
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getCurrentSeverity(key: AlarmKey): Future[FullAlarmSeverity]

  /**
   * To get a single severity representing worst alarm severity of all severities for the given key.
   *
   * @note Aggregated severity is highest severity amongst all active alarms for given key.
   * @param key represents subsystem, component or a specific alarm in alarm store
   * @return a future which completes with aggregated severity of the given subsystem, component or given alarm
   *         or fails with [[csw.services.alarm.api.exceptions.KeyNotFoundException]]
   *         or [[csw.services.alarm.api.exceptions.InactiveAlarmException]]
   */
  def getAggregatedSeverity(key: Key): Future[FullAlarmSeverity]

  /**
   * Executes the given callback function with the aggregated severity whenever any severity changes of given key in the alarm store.
   *
   * @note Aggregated severity is highest severity amongst all active alarms for given key.
   * @param key represents subsystem, component or a specific alarm in alarm store
   * @param callback which will be executed with aggregated severity on every severity change for given key
   * @return alarm subscription to ready or unsubscribe to the severity change for give key or throws
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]] or
   *         [[csw.services.alarm.api.exceptions.InactiveAlarmException]]
   */
  def subscribeAggregatedSeverityCallback(key: Key, callback: FullAlarmSeverity ⇒ Unit): AlarmSubscription

  /**
   * Sends message to the given actor as the aggregated severity whenever any severity changes of given key in the alarm store.
   *
   * @note Aggregated severity is highest severity amongst all active alarms for given key.
   * @param key represents subsystem, component or a specific alarm in alarm store
   * @param actorRef of actor which will receive message of aggregated severity on every severity change for given key
   * @return alarm subscription to ready or unsubscribe to the severity change for give key or throws
   *         [[csw.services.alarm.api.exceptions.KeyNotFoundException]] or
   *         [[csw.services.alarm.api.exceptions.InactiveAlarmException]]
   */
  def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[FullAlarmSeverity]): AlarmSubscription
  private[alarm] def subscribeAggregatedSeverity(key: Key): Source[FullAlarmSeverity, AlarmSubscription]
}
