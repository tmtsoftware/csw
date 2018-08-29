package csw.services.alarm.client.internal.services

import akka.Done
import akka.actor.ActorSystem
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal.{MetadataService, SeverityService, StatusService}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
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
    shelveTimeoutActorFactory.make(key ⇒ unshelve(key, cancelShelveTimeout = false), settings.shelveTimeout)(actorSystem)

  final override def getStatus(key: AlarmKey): Future[AlarmStatus] = async {
    log.debug(s"Getting status for alarm [${key.value}]")
    await(get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  final override def acknowledge(key: AlarmKey): Future[Done] = setAcknowledgementStatus(key, Acknowledged)

  final override def reset(key: AlarmKey): Future[Done] = async {
    log.debug(s"Reset alarm [${key.value}]")

    val currentSeverity = await(getCurrentSeverity(key))
    val originalStatus  = await(getStatus(key))

    val acknowledgedStatus = originalStatus.copy(
      //reset operation acknowledges alarm
      acknowledgementStatus = Acknowledged,
      //reset operation changes latched severity to current severity
      latchedSeverity = currentSeverity
    )

    if (originalStatus != acknowledgedStatus) await(setStatus(key, acknowledgedStatus))
    Done
  }

  final override def shelve(key: AlarmKey): Future[Done] = async {
    log.debug(s"Shelve alarm [${key.value}]")

    val status = await(getStatus(key))
    if (status.shelveStatus != Shelved) {
      await(setStatus(key, status.copy(shelveStatus = Shelved)))
      shelveTimeoutRef ! ScheduleShelveTimeout(key) // start shelve timeout for this alarm (default 8 AM local time)
    }
    Done
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  final override def unshelve(key: AlarmKey): Future[Done] = unshelve(key, cancelShelveTimeout = true)

  private[alarm] final override def unacknowledge(key: AlarmKey): Future[Done] = setAcknowledgementStatus(key, Unacknowledged)

  private[alarm] def updateStatusForSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Done] = async {

    val metadata                  = await(getMetadata(key))
    val originalStatus            = await(getStatus(key))
    val originalHeartbeatSeverity = await(getCurrentSeverity(key))

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
        def isAutoAckAndOkay            = metadata.isAutoAcknowledgeable && severity == Okay
        def isSeverityChangedAndNotOkay = severity != originalHeartbeatSeverity && severity != Okay

        if (isAutoAckAndOkay) targetAlarmStatus.copy(acknowledgementStatus = Acknowledged)
        else if (isSeverityChangedAndNotOkay) targetAlarmStatus.copy(acknowledgementStatus = Unacknowledged)
        else targetAlarmStatus
      }

      /**
       * Updates time of alarm if latchedSeverity has changed, otherwise it's a no op
       * @return updated AlarmStatus
       */
      def updateTime(): AlarmStatus =
        if (originalHeartbeatSeverity != severity) targetAlarmStatus.copy(alarmTime = AlarmTime())
        else targetAlarmStatus

      /**
       * Persists the given alarm status to redis if there are any changes, otherwise it's a no op
       */
      def persistChanges(): Future[Done] =
        if (originalStatus != targetAlarmStatus) {
          log.info(s"Updating alarm status from: [$originalStatus] to: [$targetAlarmStatus]")
          setStatus(key, targetAlarmStatus)
        } else Future.successful(Done)
    }

    await(
      originalStatus
        .updateLatchedSeverity()
        .updateAckStatus()
        .updateTime()
        .persistChanges()
    )
  }

  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Done] = {
    log.info(s"Updating alarm status [$alarmStatus] in alarm store")
    set(alarmKey, alarmStatus)
  }

  private[alarm] def setStatus(statusMap: Map[AlarmKey, AlarmStatus]): Future[Done] =
    Future
      .sequence(statusMap.map { case (key, status) => setStatus(key, status) })
      .map(_ => Done)

  final override private[alarm] def clearAllStatus(): Future[Done] =
    Future
      .sequence(
        Seq(
          alarmTimeApi.pdel(GlobalKey),
          shelveStatusApi.pdel(GlobalKey),
          ackStatusApi.pdel(GlobalKey),
          latchedSeverityApi.pdel(GlobalKey)
        )
      )
      .map(_ => Done)

  private def get(key: AlarmKey): Future[Option[AlarmStatus]] = {
    val alarmTimeF: Future[Option[AlarmTime]]               = alarmTimeApi.get(key)
    val ackStatusF: Future[Option[AcknowledgementStatus]]   = ackStatusApi.get(key)
    val shelveStatusF: Future[Option[ShelveStatus]]         = shelveStatusApi.get(key)
    val latchedSeverityF: Future[Option[FullAlarmSeverity]] = latchedSeverityApi.get(key)

    for (mayBeAckStatus       <- ackStatusF;
         mayBeLatchedSeverity <- latchedSeverityF;
         mayBeShelveStatus    <- shelveStatusF;
         mayBeAlarmTime       <- alarmTimeF) yield {

      val allEmpty = Seq(mayBeAckStatus, mayBeLatchedSeverity, mayBeLatchedSeverity, mayBeAlarmTime)
        .exists(x => x.isEmpty)

      if (allEmpty) None
      else {
        val defaultAlarmStatus = AlarmStatus()
        Some(
          AlarmStatus(
            mayBeAckStatus.getOrElse(defaultAlarmStatus.acknowledgementStatus),
            mayBeLatchedSeverity.getOrElse(defaultAlarmStatus.latchedSeverity),
            mayBeShelveStatus.getOrElse(defaultAlarmStatus.shelveStatus),
            mayBeAlarmTime.getOrElse(defaultAlarmStatus.alarmTime)
          )
        )
      }
    }
  }

  private def set(key: AlarmKey, alarmStatus: AlarmStatus): Future[Done] = {
    Future
      .sequence(
        Seq(
          ackStatusApi.set(key, alarmStatus.acknowledgementStatus),
          shelveStatusApi.set(key, alarmStatus.shelveStatus),
          alarmTimeApi.set(key, alarmStatus.alarmTime),
          latchedSeverityApi.set(key, alarmStatus.latchedSeverity),
        )
      )
      .map(_ => Done)
  }

  private def unshelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Done] = async {
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
    Done
  }

  private def setAcknowledgementStatus(key: AlarmKey, ackStatus: AcknowledgementStatus): Future[Done] = async {
    log.debug(s"$ackStatus alarm [${key.value}]")

    val status = await(getStatus(key))

    if (status.acknowledgementStatus != ackStatus) // save the set call if status is already set to given acknowledgement status
      await(setStatus(key, status.copy(acknowledgementStatus = ackStatus)))
    Done
  }

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
