package csw.alarm.client.internal.services

import akka.Done
import com.typesafe.config.Config
import csw.alarm.api.exceptions.KeyNotFoundException
import csw.alarm.api.internal.{AlarmMetadataSet, MetadataKey, MetadataService, StatusService}
import csw.alarm.client.internal.AlarmServiceLogger
import csw.alarm.client.internal.configparser.ConfigParser
import csw.alarm.client.internal.redis.RedisConnectionsFactory
import csw.alarm.models.ActivationStatus.{Active, Inactive}
import csw.alarm.models.Key.{AlarmKey, GlobalKey}
import csw.alarm.models.{AlarmMetadata, AlarmStatus, Key}
import romaine.RedisResult

import scala.async.Async.{async, await}
import scala.concurrent.Future

private[client] trait MetadataServiceModule extends MetadataService {
  self: StatusService =>

  val redisConnectionsFactory: RedisConnectionsFactory
  import redisConnectionsFactory._

  private val log = AlarmServiceLogger.getLogger

  final override def activate(alarmKey: AlarmKey): Future[Done] =
    async {
      log.debug(s"Activate alarm [${alarmKey.value}]")

      val metadata = await(metadataApi.get(alarmKey)).getOrElse(logAndThrow(KeyNotFoundException(alarmKey)))
      if (!metadata.isActive) await(metadataApi.set(alarmKey, metadata.copy(activationStatus = Active)))
      Done
    }

  final override def deactivate(alarmKey: AlarmKey): Future[Done] =
    async {
      log.debug(s"Deactivate alarm [${alarmKey.value}]")

      val metadata = await(metadataApi.get(alarmKey)).getOrElse(logAndThrow(KeyNotFoundException(alarmKey)))
      if (metadata.isActive) await(metadataApi.set(alarmKey, metadata.copy(activationStatus = Inactive)))
      Done
    }

  final override def getMetadata(alarmKey: AlarmKey): Future[AlarmMetadata] =
    async {
      log.debug(s"Getting metadata for alarm [${alarmKey.value}]")

      await(metadataApi.get(alarmKey)).getOrElse(logAndThrow(KeyNotFoundException(alarmKey)))
    }

  final override def getMetadata(key: Key): Future[List[AlarmMetadata]] =
    async {
      log.debug(s"Getting metadata for alarms matching [${key.value}]")

      val metadataKeys = await(metadataApi.keys(key))
      if (metadataKeys.isEmpty) logAndThrow(KeyNotFoundException(key))
      await(metadataApi.mget(metadataKeys)).collect { case RedisResult(_, Some(metadata)) => metadata }
    }

  final override def initAlarms(inputConfig: Config, reset: Boolean): Future[Done] =
    async {
      log.debug(s"Initializing alarm store with reset [$reset] and alarms [$inputConfig]")
      val alarmMetadataSet = ConfigParser.parseAlarmMetadataSet(inputConfig)
      if (reset) await(clearAlarmStore())
      await(feedAlarmStore(alarmMetadataSet))
    }

  private def feedAlarmStore(alarmMetadataSet: AlarmMetadataSet): Future[Done] = {
    val alarms      = alarmMetadataSet.alarms
    val metadataMap = alarms.map(metadata => MetadataKey.fromAlarmKey(metadata.alarmKey) -> metadata).toMap
    val statusMap: Map[AlarmKey, AlarmStatus] = alarms.map(metadata => metadata.alarmKey -> AlarmStatus()).toMap

    log.info(s"Feeding alarm metadata in alarm store for following alarms: [${alarms.map(_.alarmKey.value).mkString("\n")}]")
    Future
      .sequence(
        List(
          metadataApi.mset(metadataMap),
          setStatus(statusMap)
        )
      )
      .map(_ => Done)
  }

  private[alarm] def clearAlarmStore(): Future[Done] = {
    log.debug("Clearing alarm store")
    Future
      .sequence(
        List(
          metadataApi.pdel(GlobalKey),
          clearAllStatus(),
          severityApi.pdel(GlobalKey)
        )
      )
      .map(_ => Done)
  }

  private[alarm] def setMetadata(alarmKey: AlarmKey, alarmMetadata: AlarmMetadata): Future[Done] =
    metadataApi.set(alarmKey, alarmMetadata)

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
