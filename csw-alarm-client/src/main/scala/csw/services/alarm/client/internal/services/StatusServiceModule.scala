package csw.services.alarm.client.internal.services

import akka.actor.ActorSystem
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal.{MetadataService, SeverityService, StatusService}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.ShelveStatus.{Shelved, Unshelved}
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}

import scala.async.Async.{async, await}
import scala.concurrent.Future

trait StatusServiceModule extends StatusService {
  self: SeverityService with MetadataService ⇒

  def shelveTimeoutActorFactory: ShelveTimeoutActorFactory
  implicit val actorSystem: ActorSystem
  def settings: Settings
  val redisConnectionsFactory: RedisConnectionsFactory
  import redisConnectionsFactory._

  private val log = AlarmServiceLogger.getLogger
  private lazy val shelveTimeoutRef =
    shelveTimeoutActorFactory.make(key ⇒ unshelve(key, cancelShelveTimeout = false), settings.shelveTimeoutHourOfDay)(actorSystem)

  final override def getStatus(key: AlarmKey): Future[AlarmStatus] = async {
    val statusApi = await(statusApiF)

    log.debug(s"Getting status for alarm [${key.value}]")
    await(statusApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  final override def acknowledge(key: AlarmKey): Future[Unit] = setAcknowledgementStatus(key, Acknowledged)

  final override def reset(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Reset alarm [${key.value}]")

    val currentSeverity = await(getCurrentSeverity(key))

    val originalStatus = await(getStatus(key))

    val acknowledgedStatus = originalStatus.copy(
      //reset operation acknowledges alarm
      acknowledgementStatus = Acknowledged,
      //reset operation changes latched severity to current severity
      latchedSeverity = currentSeverity,
      //if latched severity is changing, alarm time also changes
      alarmTime =
        if (currentSeverity != originalStatus.latchedSeverity) Some(AlarmTime())
        else originalStatus.alarmTime
    )

    if (originalStatus != acknowledgedStatus) await(setStatus(key, acknowledgedStatus))
  }

  final override def shelve(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Shelve alarm [${key.value}]")

    val status = await(getStatus(key))
    if (status.shelveStatus != Shelved) {
      await(setStatus(key, status.copy(shelveStatus = Shelved)))
      shelveTimeoutRef ! ScheduleShelveTimeout(key) // start shelve timeout for this alarm (default 8 AM local time)
    }
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  final override def unshelve(key: AlarmKey): Future[Unit] = unshelve(key, cancelShelveTimeout = true)

  private[alarm] final override def unacknowledge(key: AlarmKey): Future[Unit] = setAcknowledgementStatus(key, Unacknowledged)

  private[alarm] def updateStatusForSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {

    // get alarm status
    val originalStatus = await(getStatus(key))

    // This class is not exposed outside `updateStatusForSeverity` function because
    // it's logic is strictly internal to this function.
    // Using closures & extension methods, this class provides a fluent api over AlarmStatus
    implicit class RichAndFluentAlarmStatus(targetAlarmStatus: AlarmStatus) {

      /**
       * Updates latched severity of the alarm if it's is greater than original or if original latched severity is Disconnected
       * This will be a no op if latched severity does not need to change
       * @return updated AlarmStatus
       */
      def updateLatchedSeverity(): AlarmStatus = {
        if (severity > targetAlarmStatus.latchedSeverity | originalStatus.latchedSeverity == Disconnected)
          targetAlarmStatus.copy(latchedSeverity = severity)
        else targetAlarmStatus
      }

      /**
       * Updates AcknowledgementStatus of alarm if severity is changed to anything but Okay
       * This will be a no op if AcknowledgementStatus does not need to change
       * @return
       */
      def updateAckStatus(): AlarmStatus = {
        if (originalStatus.latchedSeverity != targetAlarmStatus.latchedSeverity && targetAlarmStatus.latchedSeverity != Okay)
          targetAlarmStatus.copy(acknowledgementStatus = Unacknowledged)
        else targetAlarmStatus
      }

      /**
       * Updates time of alarm if latchedSeverity has changed, otherwise it's a no op
       * @return updated AlarmStatus
       */
      def updateTime(): AlarmStatus = {
        if (originalStatus.latchedSeverity != targetAlarmStatus.latchedSeverity)
          targetAlarmStatus.copy(alarmTime = Some(AlarmTime()))
        else targetAlarmStatus
      }

      /**
       * Persists the given alarm status to redis if there are any changes, otherwise it's a no op
       */
      def persistChanges(): Future[Unit] =
        if (originalStatus != targetAlarmStatus) setStatus(key, targetAlarmStatus)
        else Future(())
    }

    await(
      originalStatus
        .updateLatchedSeverity()
        .updateAckStatus()
        .updateTime()
        .persistChanges()
    )
  }

  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Unit] = {
    log.info(s"Updating alarm status [$alarmStatus] in alarm store")
    statusApiF.flatMap(_.set(alarmKey, alarmStatus))
  }

  private def unshelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Unit] = async {
    log.debug(s"Un-shelve alarm [${key.value}]")

    val status = await(getStatus(key))
    if (status.shelveStatus != Unshelved) {
      await(setStatus(key, status.copy(shelveStatus = Unshelved)))
      // if in case of manual un-shelve operation, cancel the scheduled timer for this alarm
      // this method is also called when scheduled timer for shelving of an alarm goes off (i.e. default 8 AM local time) with
      // cancelShelveTimeout as false
      // so, at this time `CancelShelveTimeout` should not be sent to `shelveTimeoutRef` as it is already cancelled
      if (cancelShelveTimeout) shelveTimeoutRef ! CancelShelveTimeout(key)
    }
  }

  private def setAcknowledgementStatus(key: AlarmKey, ackStatus: AcknowledgementStatus): Future[Unit] = async {
    log.debug(s"$ackStatus alarm [${key.value}]")

    val status = await(getStatus(key))

    if (status.acknowledgementStatus != ackStatus) // save the set call if status is already set to given acknowledgement status
      await(setStatus(key, status.copy(acknowledgementStatus = ackStatus)))
  }

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
