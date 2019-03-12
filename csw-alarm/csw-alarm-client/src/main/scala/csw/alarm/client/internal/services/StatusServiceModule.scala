package csw.alarm.client.internal.services

import java.time.Clock

import akka.Done
import akka.actor.ActorSystem
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.api.internal._
import csw.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.alarm.api.models.AlarmSeverity.Okay
import csw.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.alarm.api.models.ShelveStatus.{Shelved, Unshelved}
import csw.alarm.api.models._
import csw.alarm.client.internal.AlarmServiceLogger
import csw.alarm.client.internal.commons.Settings
import csw.alarm.client.internal.extensions.TimeExtensions.RichClock
import csw.alarm.client.internal.models.Alarm
import csw.alarm.client.internal.redis.RedisConnectionsFactory
import csw.time.core.models.UTCTime

import scala.async.Async.{async, await}
import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.Future

trait StatusServiceModule extends StatusService {
  self: SeverityService with MetadataService ⇒

  implicit val actorSystem: ActorSystem
  def settings: Settings
  val redisConnectionsFactory: RedisConnectionsFactory
  import redisConnectionsFactory._

  private val log = AlarmServiceLogger.getLogger

  final override def getStatus(alarmKey: AlarmKey): Future[AlarmStatus] = async {
    log.debug(s"Getting status for alarm [${alarmKey.value}]")
    val ackStatusF: Future[Option[AcknowledgementStatus]]   = ackStatusApi.get(alarmKey)
    val latchedSeverityF: Future[Option[FullAlarmSeverity]] = latchedSeverityApi.get(alarmKey)
    val shelveStatusF: Future[ShelveStatus]                 = getShelveStatus(alarmKey)
    val alarmTimeF: Future[Option[UTCTime]]                 = alarmTimeApi.get(alarmKey)
    val initializingF: Future[Option[Boolean]]              = initializingApi.get(alarmKey)

    val defaultAlarmStatus = AlarmStatus()
    AlarmStatus(
      await(ackStatusF).getOrElse(defaultAlarmStatus.acknowledgementStatus),
      await(latchedSeverityF).getOrElse(defaultAlarmStatus.latchedSeverity),
      await(shelveStatusF),
      await(alarmTimeF).getOrElse(defaultAlarmStatus.alarmTime),
      await(initializingF).getOrElse(defaultAlarmStatus.initializing)
    )
  }

  final override def acknowledge(alarmKey: AlarmKey): Future[Done] = setAcknowledgementStatus(alarmKey, Acknowledged)

  final override def reset(alarmKey: AlarmKey): Future[Done] = async {
    log.debug(s"Reset alarm [${alarmKey.value}]")

    val currentSeverity = await(getCurrentSeverity(alarmKey))
    val originalStatus  = await(getStatus(alarmKey))

    val acknowledgedStatus = originalStatus.copy(
      //reset operation acknowledges alarm
      acknowledgementStatus = Acknowledged,
      //reset operation changes latched severity to current severity
      latchedSeverity = currentSeverity
    )

    if (originalStatus != acknowledgedStatus) await(setStatus(alarmKey, acknowledgedStatus))
    Done
  }

  final override def shelve(alarmKey: AlarmKey): Future[Done] = {
    log.debug(s"Shelve alarm [${alarmKey.value}]")
    // it is important to have this public method call the private method with the shelveTimeout to ensure the timeout
    // functionality.  Future programmers: change with care.
    shelve(alarmKey, settings.shelveTimeout)
  }

  private[alarm] def shelve(alarmKey: AlarmKey, shelveTimeout: String): Future[Done] = {
    val clock = Clock.systemUTC()

    val shelveTTLInSeconds = clock.untilNext(shelveTimeout).toScala.toSeconds
    // delete shelve key for this alarm on configured time (default 8 AM local time), when key is expired, it will be inferred as Unshelved
    shelveStatusApi.setex(alarmKey, shelveTTLInSeconds, Shelved)
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  final override def unshelve(alarmKey: AlarmKey): Future[Done] = {
    log.debug(s"Un-shelve alarm [${alarmKey.value}]")
    shelveStatusApi.set(alarmKey, Unshelved)
  }

  // this method is expected to be called from alarm server when it receives removed event for any alarm
  final override def latchToDisconnected(alarmKey: AlarmKey, currentHeartbeatSeverity: FullAlarmSeverity): Future[Done] =
    updateStatusForSeverity(alarmKey, currentHeartbeatSeverity, Disconnected)

  private[alarm] final override def unacknowledge(alarmKey: AlarmKey): Future[Done] =
    setAcknowledgementStatus(alarmKey, Unacknowledged)

  private[alarm] def updateStatusForSeverity(
      alarmKey: AlarmKey,
      currentHeartbeatSeverity: FullAlarmSeverity,
      newHeartbeatSeverity: FullAlarmSeverity
  ): Future[Done] = async {

    val metadata       = await(getMetadata(alarmKey))
    val originalStatus = await(getStatus(alarmKey))

    // This class is not exposed outside `updateStatusForSeverity` function because
    // it's logic is strictly internal to this function.
    // Using closures & extension methods, this class provides a fluent api over AlarmStatus
    implicit class RichAndFluentAlarmStatus(targetAlarmStatus: AlarmStatus) {

      /**
       * Updates latched severity of the alarm if it's is greater than original or if component is initializing (component has not sent any heartbeat yet)
       * This will be a no op if latched severity does not need to change
       * @return updated AlarmStatus
       */
      def updateLatchedSeverity(): AlarmStatus = {
        if (newHeartbeatSeverity > targetAlarmStatus.latchedSeverity | originalStatus.initializing)
          targetAlarmStatus.copy(latchedSeverity = newHeartbeatSeverity)
        else targetAlarmStatus
      }

      /**
       * Updates AcknowledgementStatus of alarm if severity is changed to anything but Okay
       * This will be a no op if AcknowledgementStatus does not need to change
       * @return updated AlarmStatus
       */
      def updateAckStatus(): AlarmStatus = {
        def isAutoAckAndOkay            = metadata.isAutoAcknowledgeable && newHeartbeatSeverity == Okay
        def isSeverityChangedAndNotOkay = newHeartbeatSeverity != currentHeartbeatSeverity && newHeartbeatSeverity != Okay

        if (isAutoAckAndOkay) targetAlarmStatus.copy(acknowledgementStatus = Acknowledged)
        else if (isSeverityChangedAndNotOkay) targetAlarmStatus.copy(acknowledgementStatus = Unacknowledged)
        else targetAlarmStatus
      }

      /**
       * Updates time of alarm if current severity has changed, otherwise it's a no op
       * @return updated AlarmStatus
       */
      def updateTime(): AlarmStatus =
        if (currentHeartbeatSeverity != newHeartbeatSeverity) targetAlarmStatus.copy(alarmTime = UTCTime.now())
        else targetAlarmStatus

      /**
       * Sets the initializing flag to false.
       * @return updated AlarmStatus
       */
      def removeInitializingFlag(): AlarmStatus = targetAlarmStatus.copy(initializing = false)

      /**
       * Persists the given alarm status to redis if there are any changes, otherwise it's a no op
       */
      def persistChanges(): Future[Done] =
        if (originalStatus != targetAlarmStatus) {
          log.info(s"Updating alarm status from: [$originalStatus] to: [$targetAlarmStatus]")
          setStatus(alarmKey, targetAlarmStatus)
        } else Future.successful(Done)
    }

    await(
      originalStatus
        .updateLatchedSeverity()
        .updateAckStatus()
        .updateTime()
        .removeInitializingFlag()
        .persistChanges()
    )
  }

  private[alarm] def setStatus(alarmKey: AlarmKey, alarmStatus: AlarmStatus): Future[Done] = {
    log.info(s"Updating alarm status [$alarmStatus] in alarm store")
    Future
      .sequence(
        Seq(
          ackStatusApi.set(alarmKey, alarmStatus.acknowledgementStatus),
          shelveStatusApi.set(alarmKey, alarmStatus.shelveStatus),
          alarmTimeApi.set(alarmKey, alarmStatus.alarmTime),
          latchedSeverityApi.set(alarmKey, alarmStatus.latchedSeverity),
          initializingApi.set(alarmKey, alarmStatus.initializing)
        )
      )
      .map(_ ⇒ Done)
  }

  private[alarm] def setStatus(statusMap: Map[AlarmKey, AlarmStatus]): Future[Done] =
    Future
      .sequence(statusMap.map { case (key, status) ⇒ setStatus(key, status) })
      .map(_ ⇒ Done)

  final override private[alarm] def clearAllStatus(): Future[Done] =
    Future
      .sequence(
        Seq(
          alarmTimeApi.pdel(GlobalKey),
          shelveStatusApi.pdel(GlobalKey),
          ackStatusApi.pdel(GlobalKey),
          latchedSeverityApi.pdel(GlobalKey),
          initializingApi.pdel(GlobalKey)
        )
      )
      .map(_ ⇒ Done)

  private def getShelveStatus(alarmKey: AlarmKey): Future[ShelveStatus] = async {
    if (await(metadataApi.exists(alarmKey))) await(shelveStatusApi.get(alarmKey)).getOrElse(Unshelved)
    else logAndThrow(KeyNotFoundException(alarmKey))
  }

  private[alarm] def getAlarms(key: Key): Future[List[Alarm]] = metadataApi.keys(key).flatMap {
    Future.traverse(_) { key ⇒
      for {
        metadata ← getMetadata(key)
        status   ← getStatus(key)
        severity ← getCurrentSeverity(key)
      } yield Alarm(key, metadata, status, severity)
    }
  }

  private def setAcknowledgementStatus(alarmKey: AlarmKey, ackStatus: AcknowledgementStatus): Future[Done] = async {
    log.debug(s"$ackStatus alarm [${alarmKey.value}]")

    val status = await(getStatus(alarmKey))

    if (status.acknowledgementStatus != ackStatus) // save the set call if status is already set to given acknowledgement status
      await(setStatus(alarmKey, status.copy(acknowledgementStatus = ackStatus)))
    Done
  }

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
