package csw.services.alarm.client.internal.services

import com.typesafe.config.Config
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal.{MetadataKey, StatusKey}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.{AlarmMetadata, AlarmMetadataSet, AlarmStatus, Key}
import csw.services.alarm.api.scaladsl.MetadataService
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.configparser.ConfigParser
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory

import scala.async.Async.{async, await}
import scala.concurrent.Future

trait MetadataServiceModule extends MetadataService {

  val redisConnectionsFactory: RedisConnectionsFactory
  import redisConnectionsFactory._

  private val log = AlarmServiceLogger.getLogger

  final override def activate(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Activate alarm [${key.value}]")
    val metadataApi = await(metadataApiF)

    val metadata = await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
    if (!metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Active)))
  }

  final override def deActivate(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Deactivate alarm [${key.value}]")
    val metadataApi = await(metadataApiF)

    val metadata = await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
    if (metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Inactive)))
  }

  final override def getMetadata(key: AlarmKey): Future[AlarmMetadata] = async {
    log.debug(s"Getting metadata for alarm [${key.value}]")
    val metadataApi = await(metadataApiF)

    await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  final override def getMetadata(key: Key): Future[List[AlarmMetadata]] = async {
    log.debug(s"Getting metadata for alarms matching [${key.value}]")
    val metadataApi = await(metadataApiF)

    val metadataKeys = await(metadataApi.keys(key))
    if (metadataKeys.isEmpty) logAndThrow(KeyNotFoundException(key))
    await(metadataApi.mget(metadataKeys)).map(_.getValue)
  }

  final override def initAlarms(inputConfig: Config, reset: Boolean): Future[Unit] = async {
    log.debug(s"Initializing alarm store with reset [$reset]")
    val alarmMetadataSet = ConfigParser.parseAlarmMetadataSet(inputConfig)
    if (reset) await(resetAlarmStore())
    await(setAlarmStore(alarmMetadataSet))
  }

  private def setAlarmStore(alarmMetadataSet: AlarmMetadataSet) = {
    val alarms      = alarmMetadataSet.alarms
    val metadataMap = alarms.map(metadata ⇒ MetadataKey.fromAlarmKey(metadata.alarmKey) → metadata).toMap
    val statusMap   = alarms.map(metadata ⇒ StatusKey.fromAlarmKey(metadata.alarmKey) → AlarmStatus()).toMap

    log.info(s"Feeding alarm metadata in alarm store for following alarms: [${alarms.map(_.alarmKey.value).mkString("\n")}]")
    Future.sequence(
      List(
        metadataApiF.flatMap(_.mset(metadataMap)),
        statusApiF.flatMap(_.mset(statusMap))
      )
    )
  }

  private def resetAlarmStore() = {
    log.debug("Resetting alarm store")
    Future
      .sequence(
        List(
          metadataApiF.flatMap(_.pdel(GlobalKey)),
          statusApiF.flatMap(_.pdel(GlobalKey)),
          severityApiF.flatMap(_.pdel(GlobalKey))
        )
      )
  }

  private[alarm] def setMetadata(alarmKey: AlarmKey, alarmMetadata: AlarmMetadata): Future[Unit] =
    metadataApiF.flatMap(_.set(alarmKey, alarmMetadata))

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
