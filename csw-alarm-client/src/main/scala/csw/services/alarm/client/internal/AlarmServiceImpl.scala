package csw.services.alarm.client.internal

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.services.alarm.api.exceptions.{InvalidSeverityException, ResetOperationFailedException}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}
import csw.services.alarm.api.models._
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.client.internal.AlarmTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}
import csw.services.alarm.client.internal.codec.{AlarmMetadataCodec, AlarmSeverityCodec, AlarmStatusCodec}
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future

class AlarmServiceImpl(
    redisURI: RedisURI,
    redisClient: RedisClient,
    shelveTimeoutActorFactory: ShelveTimeoutActorFactory
)(implicit actorSystem: ActorSystem)
    extends AlarmAdminService {

  import actorSystem.dispatcher

  private lazy val asyncMetadataApiF: Future[RedisAsyncCommands[AlarmKey, AlarmMetadata]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(AlarmMetadataCodec, redisURI).toScala)
    .map(_.async())

  private lazy val asyncSeverityApiF: Future[RedisAsyncCommands[AlarmKey, AlarmSeverity]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(AlarmSeverityCodec, redisURI).toScala)
    .map(_.async())

  private lazy val asyncStatusApiF: Future[RedisAsyncCommands[AlarmKey, AlarmStatus]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(AlarmStatusCodec, redisURI).toScala)
    .map(_.async())

  private val refreshInSeconds       = actorSystem.settings.config.getInt("alarm.refresh-in-seconds") // default value is 5 seconds
  private val maxMissedRefreshCounts = actorSystem.settings.config.getInt("alarm.max-missed-refresh-counts") //default value is 3 times
  private val ttlInSeconds           = refreshInSeconds * maxMissedRefreshCounts

  private lazy val shelveTimeoutRef = shelveTimeoutActorFactory.make(unShelve(_, cancelShelveTimeout = false))

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    // get alarm metadata
    val metadataApi = await(asyncMetadataApiF)
    val alarm       = await(metadataApi.get(key).toScala)

    // validate if the provided severity is supported by this alarm
    if (!alarm.supportedSeverities.contains(severity))
      throw InvalidSeverityException(key, alarm.supportedSeverities, severity)

    // get the current severity of the alarm
    val severityApi     = await(asyncSeverityApiF)
    val currentSeverity = await(severityApi.get(key).toScala)

    // set the severity of the alarm so that it does not transition to `Disconnected` state
    await(severityApi.setex(key, ttlInSeconds, severity).toScala)

    // get alarm status
    val statusApi = await(asyncStatusApiF)
    var status    = await(statusApi.get(key).toScala)

    // derive latch status
    if (alarm.isLatchable && severity.isHighRisk && severity.isHigherThan(status.latchedSeverity))
      status = status.copy(latchStatus = Latched, latchedSeverity = severity)

    // derive acknowledgement status
    if (severity.isHighRisk && severity != currentSeverity) {
      if (alarm.isAutoAcknowledgable) status = status.copy(acknowledgementStatus = Acknowledged)
      else status = status.copy(acknowledgementStatus = UnAcknowledged)
    }

    // update alarm status
    await(statusApi.set(key, status).toScala)
  }

  override def getSeverity(key: AlarmKey): Future[AlarmSeverity] = async {
    val severityApi = await(asyncSeverityApiF)
    await(severityApi.get(key).toScala)
  }

  override def getMetadata(key: AlarmKey): Future[AlarmMetadata] = async {
    val metadataApi = await(asyncMetadataApiF)
    await(metadataApi.get(key).toScala)
  }

  override def getStatus(key: AlarmKey): Future[AlarmStatus] = async {
    val statusApi = await(asyncStatusApiF)
    await(statusApi.get(key).toScala)
  }

  override def acknowledge(key: AlarmKey): Future[Unit] = async {
    val statusApi = await(asyncStatusApiF)
    val status    = await(statusApi.get(key).toScala)
    if (status.acknowledgementStatus == UnAcknowledged) // save the set call if status is already Acknowledged
      await(statusApi.set(key, status.copy(acknowledgementStatus = Acknowledged)).toScala)
  }

  override def reset(key: AlarmKey): Future[Unit] = async {

    val severityApi     = await(asyncSeverityApiF)
    val currentSeverity = await(severityApi.get(key).toScala)

    if (currentSeverity != Okay) throw ResetOperationFailedException(key, currentSeverity)

    val statusApi = await(asyncStatusApiF)
    val status    = await(statusApi.get(key).toScala)

    // reset is only called when severity is `Okay` so update acknowledgeStatus to `Acknowledged`
    if (status.acknowledgementStatus == UnAcknowledged) // save the set call if status is already Acknowledged
      await(statusApi.set(key, status.copy(acknowledgementStatus = Acknowledged)).toScala)

    // reset is only called when severity is `Okay` so update LatchStatus to `UnLatched`
    if (status.latchStatus == Latched) {
      await(statusApi.set(key, status.copy(latchStatus = UnLatched)).toScala)
    }
  }

  override def shelve(key: AlarmKey): Future[Unit] = async {
    val severityApi     = await(asyncSeverityApiF)
    val currentSeverity = await(severityApi.get(key).toScala)

    // TODO: confirm
    if (currentSeverity.isHighRisk) {
      val statusApi = await(asyncStatusApiF)
      val status    = await(statusApi.get(key).toScala)
      if (status.shelveStatus != Shelved) {
        await(statusApi.set(key, status.copy(shelveStatus = Shelved)).toScala)
        shelveTimeoutRef ! ScheduleShelveTimeout(key) // start shelve timeout for this alarm (default 8 AM local time)
      }
    }
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  override def unShelve(key: AlarmKey): Future[Unit] = async {
    unShelve(key, cancelShelveTimeout = true)
  }

  private def unShelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Unit] = async {
    //TODO: decide whether to unshelve an alarm when it goes to okay
    val statusApi = await(asyncStatusApiF)
    val status    = await(statusApi.get(key).toScala)
    if (status.shelveStatus != UnShelved) {
      await(statusApi.set(key, status.copy(shelveStatus = UnShelved)).toScala)
      // if in case of manual un-shelve operation, cancel the scheduled timer for this alarm
      // this method is also called when scheduled timer for shelving of an alarm goes off (i.e. default 8 AM local time) with
      // cancelShelveTimeout as false
      // so, at this time `CancelShelveTimeout` should not be sent to `shelveTimeoutRef` as it is already cancelled
      if (cancelShelveTimeout) shelveTimeoutRef ! CancelShelveTimeout(key)
    }
  }

  override def activate(key: AlarmKey): Future[Unit] = async {
    val statusApi = await(asyncStatusApiF)
    val status    = await(statusApi.get(key).toScala)

    if (status.activationStatus != Active) await(statusApi.set(key, status.copy(activationStatus = Active)).toScala)
  }

  override def deActivate(key: AlarmKey): Future[Unit] = async {
    val statusApi = await(asyncStatusApiF)
    val status    = await(statusApi.get(key).toScala)

    if (status.activationStatus != Inactive) await(statusApi.set(key, status.copy(activationStatus = Inactive)).toScala)
  }
}
