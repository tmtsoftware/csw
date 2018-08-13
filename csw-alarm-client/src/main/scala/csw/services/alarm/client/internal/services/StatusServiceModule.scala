package csw.services.alarm.client.internal.services

import acyclic.skipped
import csw.services.alarm.api.exceptions.{KeyNotFoundException, ResetOperationNotAllowed}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}
import csw.services.alarm.api.models._
import csw.services.alarm.api.scaladsl.StatusService
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}

import scala.async.Async.{async, await}
import scala.concurrent.Future

trait StatusServiceModule extends StatusService {
  self: SeverityServiceModule with MetadataServiceModule ⇒

  def shelveTimeoutActorFactory: ShelveTimeoutActorFactory
  def settings: Settings
  import redisConnectionsFactory._

  private val log = AlarmServiceLogger.getLogger
  private lazy val shelveTimeoutRef =
    shelveTimeoutActorFactory.make(key ⇒ unShelve(key, cancelShelveTimeout = false))(actorSystem)

  final override def getStatus(key: AlarmKey): Future[AlarmStatus] = async {
    val statusApi = await(statusApiF)

    log.debug(s"Getting status for alarm [${key.value}]")
    await(statusApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  final override def acknowledge(key: AlarmKey): Future[Unit] = setAcknowledgementStatus(key, Acknowledged)

  // reset is only called when severity is `Okay`
  final override def reset(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Reset alarm [${key.value}]")
    val metadata = await(getMetadata(key))

    val currentSeverity = await(getCurrentSeverity(key))
    if (currentSeverity != Okay) logAndThrow(ResetOperationNotAllowed(key, currentSeverity))

    val status = await(getStatus(key))
    val resetStatus = status.copy(
      acknowledgementStatus = Acknowledged,
      latchStatus = if (metadata.isLatchable) Latched else UnLatched,
      latchedSeverity = Okay,
      alarmTime = alarmTime(status)
    )
    if (status != resetStatus) await(setStatus(key, resetStatus))
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
  final override def unShelve(key: AlarmKey): Future[Unit] = unShelve(key, cancelShelveTimeout = true)

  private[alarm] final override def unAcknowledge(key: AlarmKey): Future[Unit] = setAcknowledgementStatus(key, UnAcknowledged)

  private[alarm] def updateStatusForSeverity(
      key: AlarmKey,
      severity: AlarmSeverity,
      previousSeverity: AlarmSeverity
  ): Future[Unit] = async {
    // get alarm metadata
    val alarm = await(getMetadata(key))

    // get alarm status
    val status = await(getStatus(key))

    object Latchable {
      def unapply(alarmMetadata: AlarmMetadata): Boolean = alarmMetadata.isLatchable
      def unapply(alarmSeverity: AlarmSeverity): Boolean = alarmSeverity.latchable
    }

    object NotLatchable {
      def unapply(alarmMetadata: AlarmMetadata): Boolean = !alarmMetadata.isLatchable
      def unapply(alarmSeverity: AlarmSeverity): Boolean = !alarmSeverity.latchable
    }

    object IsUnLatched {
      def unapply(status: AlarmStatus): Boolean = status.latchStatus == LatchStatus.UnLatched
    }

    object HigherLatchedSeverity {
      def unapply(status: AlarmStatus): Boolean = severity > status.latchedSeverity
    }

    object NotPreviousSeverity {
      def unapply(severity: AlarmSeverity): Boolean = severity != previousSeverity
    }

    val setLatchSeverity = status.copy(latchedSeverity = severity, alarmTime = Some(AlarmTime()))
    val setLatchedSeverityAndStatus: AlarmStatus =
      status.copy(latchedSeverity = severity, alarmTime = Some(AlarmTime()), latchStatus = Latched)

    val updatedStatus = (alarm, severity, status) match {
      case (Latchable(), Latchable(), HigherLatchedSeverity()) ⇒ setLatchedSeverityAndStatus
      case (Latchable(), Latchable(), IsUnLatched())           ⇒ setLatchedSeverityAndStatus
      case (NotLatchable(), NotPreviousSeverity(), _)          ⇒ setLatchSeverity
      case _                                                   ⇒ status
    }

    // derive acknowledgement status

    object IsAutoAcknowledgeable {
      def unapply(alarm: AlarmMetadata): Boolean = alarm.isAutoAcknowledgeable
    }

    object IsLatchedSeverityOkay {
      def unapply(status: AlarmStatus): Boolean = status.latchedSeverity == Okay
    }

    object IsOtherSeverity {
      def unapply(status: AlarmStatus): Boolean = status.latchedSeverity != previousSeverity
    }

    val acknowledgedStatus   = updatedStatus.copy(acknowledgementStatus = Acknowledged)
    val unAcknowledgedStatus = updatedStatus.copy(acknowledgementStatus = UnAcknowledged)

    val newStatus = (alarm, updatedStatus) match {
      case (IsAutoAcknowledgeable(), _) ⇒ acknowledgedStatus
      case (_, IsLatchedSeverityOkay()) ⇒ acknowledgedStatus
      case (_, IsOtherSeverity())       ⇒ unAcknowledgedStatus
      case _                            ⇒ updatedStatus
    }

    // update alarm status (with recent time) only when severity changes
    if (newStatus != status) await(setStatus(key, newStatus))
  }

  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Unit] = {
    log.info(s"Updating alarm status [$alarmStatus] in alarm store")
    statusApiF.flatMap(_.set(alarmKey, alarmStatus))
  }

  private def unShelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Unit] = async {
    log.debug(s"Un-shelve alarm [${key.value}]")

    //TODO: decide whether to  unshelve an alarm when it goes to okay
    val status = await(getStatus(key))
    if (status.shelveStatus != UnShelved) {
      await(setStatus(key, status.copy(shelveStatus = UnShelved)))
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

  private def alarmTime(status: AlarmStatus) = if (status.latchedSeverity != Okay) Some(AlarmTime()) else status.alarmTime

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
