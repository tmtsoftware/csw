package csw.services.alarm.client.internal.services

import akka.Done
import com.typesafe.config.Config
import csw.services.alarm.api.exceptions.KeyNotFoundException
import csw.services.alarm.api.internal.{MetadataKey, MetadataService, StatusService}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.{AlarmMetadata, AlarmMetadataSet, AlarmStatus, Key}
import csw.services.alarm.client.internal.AlarmServiceLogger
import csw.services.alarm.client.internal.configparser.ConfigParser
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import romaine.reactive.RedisResult

import scala.async.Async.{async, await}
import scala.concurrent.Future

trait MetadataServiceModule extends MetadataService {
  self: StatusService ⇒

  val redisConnectionsFactory: RedisConnectionsFactory
  import redisConnectionsFactory._

  private val log = AlarmServiceLogger.getLogger

  final override def activate(key: AlarmKey): Future[Done] = async {
    log.debug(s"Activate alarm [${key.value}]")

    val metadata = await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
    if (!metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Active)))
    Done
  }

  final override def deactivate(key: AlarmKey): Future[Done] = async {
    log.debug(s"Deactivate alarm [${key.value}]")

    val metadata = await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
    if (metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Inactive)))
    Done
  }

  final override def getMetadata(key: AlarmKey): Future[AlarmMetadata] = async {
    log.debug(s"Getting metadata for alarm [${key.value}]")

    await(metadataApi.get(key)).getOrElse(logAndThrow(KeyNotFoundException(key)))
  }

  final override def getMetadata(key: Key): Future[List[AlarmMetadata]] = async {
    log.debug(s"Getting metadata for alarms matching [${key.value}]")

    val metadataKeys = await(metadataApi.keys(key))
    if (metadataKeys.isEmpty) logAndThrow(KeyNotFoundException(key))
    await(metadataApi.mget(metadataKeys)).collect { case RedisResult(_, Some(metadata)) ⇒ metadata }
  }

  final override def initAlarms(inputConfig: Config, reset: Boolean): Future[Done] = async {
    log.debug(s"Initializing alarm store with reset [$reset] and alarms [$inputConfig]")
    val alarmMetadataSet = ConfigParser.parseAlarmMetadataSet(inputConfig)
    if (reset) await(resetAlarmStore())
    await(setAlarmStore(alarmMetadataSet))
  }

  private def setAlarmStore(alarmMetadataSet: AlarmMetadataSet): Future[Done] = {
    val alarms                                = alarmMetadataSet.alarms
    val metadataMap                           = alarms.map(metadata ⇒ MetadataKey.fromAlarmKey(metadata.alarmKey) → metadata).toMap
    val statusMap: Map[AlarmKey, AlarmStatus] = alarms.map(metadata ⇒ metadata.alarmKey → AlarmStatus()).toMap

    log.info(s"Feeding alarm metadata in alarm store for following alarms: [${alarms.map(_.alarmKey.value).mkString("\n")}]")
    Future
      .sequence(
        List(
          metadataApi.mset(metadataMap),
          setStatus(statusMap)
        )
      )
      .map(_ ⇒ Done)
  }

  private def resetAlarmStore(): Future[Done] = {
    log.debug("Resetting alarm store")
    Future
      .sequence(
        List(
          metadataApi.pdel(GlobalKey),
          clearAllStatus(),
          severityApi.pdel(GlobalKey)
        )
      )
      .map(_ ⇒ Done)
  }

  private[alarm] def setMetadata(alarmKey: AlarmKey, alarmMetadata: AlarmMetadata): Future[Done] =
    metadataApi.set(alarmKey, alarmMetadata)

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
