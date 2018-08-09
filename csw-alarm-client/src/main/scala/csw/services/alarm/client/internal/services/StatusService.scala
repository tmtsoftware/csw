package csw.services.alarm.client.internal.services

import akka.actor.ActorSystem
import csw.services.alarm.api.exceptions.{KeyNotFoundException, ResetOperationNotAllowed}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}
import csw.services.alarm.api.models.{AcknowledgementStatus, AlarmSeverity, AlarmStatus, AlarmTime}
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}

import scala.async.Async.{async, await}
import scala.concurrent.Future

class StatusService(
    redisConnectionsFactory: RedisConnectionsFactory,
    shelveTimeoutActorFactory: ShelveTimeoutActorFactory,
    metadataService: MetadataService,
    severityService: SeverityService,
    settings: Settings
)(implicit actorSystem: ActorSystem) {
  import redisConnectionsFactory._

  private val log                   = AlarmServiceLogger.getLogger
  private lazy val shelveTimeoutRef = shelveTimeoutActorFactory.make(key ⇒ unShelve(key, cancelShelveTimeout = false))

  def getStatus(key: AlarmKey): Future[AlarmStatus] = async {
    val statusApi = await(statusApiF)

    log.debug(s"Getting status for alarm [${key.value}]")
    await(statusApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  def acknowledge(key: AlarmKey): Future[Unit] = setAcknowledgementStatus(key, Acknowledged)

  private[alarm] def unAcknowledge(key: AlarmKey): Future[Unit] = setAcknowledgementStatus(key, UnAcknowledged)

  // reset is only called when severity is `Okay`
  def reset(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Reset alarm [${key.value}]")
    val metadataApi   = await(metadataApiF)
    val statusApi     = await(statusApiF)
    val maybeMetadata = await(metadataApi.get(key))

    maybeMetadata match {
      case Some(metadata) ⇒
        val currentSeverity = await(severityService.getCurrentSeverity(key))
        if (currentSeverity != Okay) logAndThrow(ResetOperationNotAllowed(key, currentSeverity))

        val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
        val resetStatus = status.copy(
          acknowledgementStatus = Acknowledged,
          latchStatus = if (metadata.isLatchable) Latched else UnLatched,
          latchedSeverity = Okay,
          alarmTime = alarmTime(status)
        )
        if (status != resetStatus) await(statusApi.set(key, resetStatus))

      case None ⇒ logAndThrow(KeyNotFoundException(key))
    }
  }

  def shelve(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Shelve alarm [${key.value}]")
    val statusApi = await(statusApiF)

    val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
    if (status.shelveStatus != Shelved) {
      await(statusApi.set(key, status.copy(shelveStatus = Shelved)))
      shelveTimeoutRef ! ScheduleShelveTimeout(key) // start shelve timeout for this alarm (default 8 AM local time)
    }
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  def unShelve(key: AlarmKey): Future[Unit] = unShelve(key, cancelShelveTimeout = true)

  private[alarm] def updateStatusForSeverity(
      key: AlarmKey,
      severity: AlarmSeverity,
      previousSeverity: AlarmSeverity
  ): Future[Unit] = async {
    // get alarm metadata
    val alarm = await(metadataService.getMetadata(key))

    // get alarm status
    val status    = await(getStatus(key))
    var newStatus = status

    def shouldUpdateLatchStatus: Boolean                     = alarm.isLatchable && severity.latchable
    def shouldUpdateLatchedSeverityWhenLatchable: Boolean    = shouldUpdateWhenLatched || shouldUpdateWhenUnLatched
    def shouldUpdateWhenLatched: Boolean                     = alarm.isLatchable && severity.latchable && severity > status.latchedSeverity
    def shouldUpdateWhenUnLatched: Boolean                   = alarm.isLatchable && status.latchStatus == UnLatched && severity.latchable
    def shouldUpdateLatchedSeverityWhenNotLatchable: Boolean = !alarm.isLatchable && severity != previousSeverity

    if (shouldUpdateLatchStatus) newStatus = newStatus.copy(latchStatus = Latched)

    if (shouldUpdateLatchedSeverityWhenLatchable || shouldUpdateLatchedSeverityWhenNotLatchable)
      newStatus = newStatus.copy(latchedSeverity = severity, alarmTime = Some(AlarmTime()))

    // derive acknowledgement status
    if (newStatus.latchedSeverity == Okay || alarm.isAutoAcknowledgeable)
      newStatus = newStatus.copy(acknowledgementStatus = Acknowledged)
    else if (severity != previousSeverity) newStatus = newStatus.copy(acknowledgementStatus = UnAcknowledged)

    // update alarm status (with recent time) only when severity changes
    if (newStatus != status) await(setStatus(key, newStatus))
  }

  /*
  def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    log.debug(
      s"Setting severity [${severity.name}] for alarm [${key.value}] with expire timeout [$settings.ttlInSeconds] seconds"
    )

    // get alarm metadata
    val alarm = await(metadataServiceImpl.getMetadata(key))

    // validate if the provided severity is supported by this alarm
    if (!alarm.allSupportedSeverities.contains(severity))
      logAndThrow(InvalidSeverityException(key, alarm.allSupportedSeverities, severity))

    // get the current severity of the alarm
    val severityApi      = await(severityApiF)
    val previousSeverity = await(severityApi.get(key)).getOrElse(Disconnected)

    // set the severity of the alarm so that it does not transition to `Disconnected` state
    log.info(s"Updating current severity [${severity.name}] in alarm store")
    await(severityApi.setex(key, settings.ttlInSeconds, severity))

    // get alarm status
    val status    = await(statusServiceImpl.getStatus(key))
    var newStatus = status

    def shouldUpdateLatchStatus: Boolean                     = alarm.isLatchable && severity.latchable
    def shouldUpdateLatchedSeverityWhenLatchable: Boolean    = shouldUpdateWhenLatched || shouldUpdateWhenUnLatched
    def shouldUpdateWhenLatched: Boolean                     = alarm.isLatchable && severity.latchable && severity > status.latchedSeverity
    def shouldUpdateWhenUnLatched: Boolean                   = alarm.isLatchable && status.latchStatus == UnLatched && severity.latchable
    def shouldUpdateLatchedSeverityWhenNotLatchable: Boolean = !alarm.isLatchable && severity != previousSeverity

    if (shouldUpdateLatchStatus) newStatus = newStatus.copy(latchStatus = Latched)

    if (shouldUpdateLatchedSeverityWhenLatchable || shouldUpdateLatchedSeverityWhenNotLatchable)
      newStatus = newStatus.copy(latchedSeverity = severity, alarmTime = Some(AlarmTime()))

    // derive acknowledgement status
    if (newStatus.latchedSeverity == Okay || alarm.isAutoAcknowledgeable)
      newStatus = newStatus.copy(acknowledgementStatus = Acknowledged)
    else if (severity != previousSeverity) newStatus = newStatus.copy(acknowledgementStatus = UnAcknowledged)

    // update alarm status (with recent time) only when severity changes
    if (newStatus != status) await(statusServiceImpl.setStatus(key, newStatus))
  }
   */

  private def unShelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Unit] = async {
    log.debug(s"Un-shelve alarm [${key.value}]")
    val statusApi = await(statusApiF)

    //TODO: decide whether to  unshelve an alarm when it goes to okay
    val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
    if (status.shelveStatus != UnShelved) {
      await(statusApi.set(key, status.copy(shelveStatus = UnShelved)))
      // if in case of manual un-shelve operation, cancel the scheduled timer for this alarm
      // this method is also called when scheduled timer for shelving of an alarm goes off (i.e. default 8 AM local time) with
      // cancelShelveTimeout as false
      // so, at this time `CancelShelveTimeout` should not be sent to `shelveTimeoutRef` as it is already cancelled
      if (cancelShelveTimeout) shelveTimeoutRef ! CancelShelveTimeout(key)
    }
  }

  private def setAcknowledgementStatus(key: AlarmKey, ackStatus: AcknowledgementStatus): Future[Unit] = async {
    log.debug(s"$ackStatus alarm [${key.value}]")

    val metadataApi = await(metadataApiF)
    val statusApi   = await(statusApiF)

    if (await(metadataApi.exists(key))) {
      val status = await(statusApi.get(key)).getOrElse(AlarmStatus())

      if (status.acknowledgementStatus != ackStatus) // save the set call if status is already set to given acknowledgement status
        await(statusApi.set(key, status.copy(acknowledgementStatus = ackStatus)))
    } else logAndThrow(KeyNotFoundException(key))
  }

  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Unit] = {
    log.info(s"Updating alarm status [$alarmStatus] in alarm store")
    statusApiF.flatMap(_.set(alarmKey, alarmStatus))
  }

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }

  private def alarmTime(status: AlarmStatus) = if (status.latchedSeverity != Okay) Some(AlarmTime()) else status.alarmTime
}
