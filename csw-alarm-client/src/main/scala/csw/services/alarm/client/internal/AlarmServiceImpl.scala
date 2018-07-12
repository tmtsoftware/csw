package csw.services.alarm.client.internal

import akka.actor.ActorSystem
import csw.services.alarm.api.exceptions.InvalidSeverityException
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.LatchStatus.Latched
import csw.services.alarm.api.models.{AlarmKey, AlarmMetadata, AlarmSeverity, AlarmStatus}
import csw.services.alarm.api.scaladsl.AlarmAdminService
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future

class AlarmServiceImpl(redisURI: RedisURI, redisClient: RedisClient)(implicit actorSystem: ActorSystem)
    extends AlarmAdminService {

  import actorSystem.dispatcher

  private lazy val asyncMetadataCommandsF: Future[RedisAsyncCommands[AlarmKey, AlarmMetadata]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(AlarmMetadataCodec, redisURI).toScala)
    .map(_.async())

  private lazy val asyncSeverityCommandsF: Future[RedisAsyncCommands[AlarmKey, AlarmSeverity]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(AlarmSeverityCodec, redisURI).toScala)
    .map(_.async())

  private lazy val asyncStatusCommandsF: Future[RedisAsyncCommands[AlarmKey, AlarmStatus]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(AlarmStatusCodec, redisURI).toScala)
    .map(_.async())

  private val refreshInSeconds       = actorSystem.settings.config.getInt("alarm.refresh-in-seconds") // default value is 5 seconds
  private val maxMissedRefreshCounts = actorSystem.settings.config.getInt("alarm.max-missed-refresh-counts") //default value is 3 times
  private val ttlInSeconds           = refreshInSeconds * maxMissedRefreshCounts

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    // get alarm metadata
    val metadataCommands = await(asyncMetadataCommandsF)
    val alarm            = await(metadataCommands.get(key).toScala)

    // validate if the provided severity is supported by this alarm
    if (!alarm.supportedSeverities.contains(severity))
      throw InvalidSeverityException(key, alarm.supportedSeverities, severity)

    // get the current severity of the alarm
    val severityCommands = await(asyncSeverityCommandsF)
    val currentSeverity  = await(severityCommands.get(key).toScala)

    // set the severity of the alarm so that it does not transition to `Disconnected` state
    await(severityCommands.setex(key, ttlInSeconds, severity).toScala)

    // get alarm status
    val statusCommands = await(asyncStatusCommandsF)
    var status         = await(statusCommands.get(key).toScala)

    // derive latch status
    if (alarm.isLatchable && severity.isHighRisk && severity.isHigherThan(status.latchedSeverity))
      status = status.copy(latchStatus = Latched, latchedSeverity = severity)

    // derive acknowledgement status
    if (severity.isHighRisk && severity != currentSeverity) {
      if (alarm.isAutoAcknowledgable) status = status.copy(acknowledgementStatus = Acknowledged)
      else status = status.copy(acknowledgementStatus = UnAcknowledged)
    }

    // update alarm status
    await(statusCommands.set(key, status).toScala)
  }

  override def getSeverity(key: AlarmKey): Future[AlarmSeverity] = ???
}
