package csw.alarm.api.javadsl

import java.util.concurrent.CompletableFuture

import akka.Done
import csw.alarm.api.models.AlarmSeverity
import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.scaladsl.AlarmService

/**
 * An AlarmService interface to set the severity of alarms
 */
trait IAlarmService {

  /**
   * This api allows to set the severity of an alarm. It also internally updates the latch severity and acknowledgement status.
   * The severity is set in alarm store with a specific TTL (time to live). After the time passes for TTL, the severity
   * will be automatically inferred as `Disconnected`.
   *
   * Note that by default all alarms are loaded in alarm store as `Disconnected`. Once the component is up and working,
   *       it will be it's responsibility to update all it's alarms with appropriate severity and keep refreshing it.
   * @param alarmKey represents a unique alarm in alarm store e.g nfiraos.trombone.tromboneaxislowlimitalarm
   * @param severity represents the severity to be set for the alarm e.g. Okay, Warning, Major, Critical, etc
   * @return a CompletableFuture which completes when the severity is successfully set in alarm store or fails with
   *         [[csw.alarm.api.exceptions.InvalidSeverityException]]
   *         or [[csw.alarm.api.exceptions.KeyNotFoundException]]
   */
  def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): CompletableFuture[Done]

  /**
   *  Returns the scala version of AlarmService
   */
  def asScala: AlarmService
}
