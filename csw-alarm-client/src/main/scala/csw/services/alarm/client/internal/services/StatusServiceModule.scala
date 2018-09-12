package csw.services.alarm.client.internal.services

import java.time.Clock

import akka.Done
import akka.actor.ActorSystem
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal._
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, Unacknowledged}
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.FullAlarmSeverity.Disconnected
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, Unshelved}
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.extensions.TimeExtensions.RichClock
import csw.services.alarm.client.internal.models.Alarm
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory

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
    val alarmTimeF: Future[Option[AlarmTime]]               = alarmTimeApi.get(alarmKey)

    val defaultAlarmStatus = AlarmStatus()
    AlarmStatus(
      await(ackStatusF).getOrElse(defaultAlarmStatus.acknowledgementStatus),
      await(latchedSeverityF).getOrElse(defaultAlarmStatus.latchedSeverity),
      await(shelveStatusF),
      await(alarmTimeF).getOrElse(defaultAlarmStatus.alarmTime)
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
    // Use the UTC timezone for the time-being. Once the time service is in place, it can query time service.
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

  private[alarm] final override def unacknowledge(alarmKey: AlarmKey): Future[Done] =
    setAcknowledgementStatus(alarmKey, Unacknowledged)

  private[alarm] def updateStatusForSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Done] = async {

    val metadata                  = await(getMetadata(alarmKey))
    val originalStatus            = await(getStatus(alarmKey))
    val originalHeartbeatSeverity = await(getCurrentSeverity(alarmKey))

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
          setStatus(alarmKey, targetAlarmStatus)
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
    Future
      .sequence(
        Seq(
          ackStatusApi.set(alarmKey, alarmStatus.acknowledgementStatus),
          shelveStatusApi.set(alarmKey, alarmStatus.shelveStatus),
          alarmTimeApi.set(alarmKey, alarmStatus.alarmTime),
          latchedSeverityApi.set(alarmKey, alarmStatus.latchedSeverity)
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
          latchedSeverityApi.pdel(GlobalKey)
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
