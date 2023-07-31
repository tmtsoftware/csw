/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.client.internal.services

import org.apache.pekko.Done
import org.apache.pekko.actor.typed
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import csw.alarm.api.exceptions.{InactiveAlarmException, InvalidSeverityException, KeyNotFoundException}
import csw.alarm.api.internal.*
import csw.alarm.api.scaladsl.AlarmSubscription
import csw.alarm.client.internal.commons.Settings
import csw.alarm.client.internal.redis.RedisConnectionsFactory
import csw.alarm.client.internal.{AlarmRomaineCodec, AlarmServiceLogger}
import csw.alarm.models.FullAlarmSeverity.Disconnected
import csw.alarm.models.Key.AlarmKey
import csw.alarm.models.{AlarmSeverity, FullAlarmSeverity, Key}
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisResult
import romaine.extensions.SourceExtensions.RichSource
import romaine.reactive.RedisSubscription

import cps.*
import scala.concurrent.Future

private[client] trait SeverityServiceModule extends SeverityService {
  self: MetadataService with StatusService =>

  val redisConnectionsFactory: RedisConnectionsFactory
  def settings: Settings
  implicit val actorSystem: typed.ActorSystem[_]
  import redisConnectionsFactory._

  private val log = AlarmServiceLogger.getLogger

  final override def setSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Done] =
    async {
      val currentSeverity = await(getCurrentSeverity(alarmKey))
      await(updateStatusForSeverity(alarmKey, currentSeverity, severity))
      await(setCurrentSeverity(alarmKey, severity))
    }

  final override def getAggregatedSeverity(key: Key): Future[FullAlarmSeverity] =
    async {
      log.debug(s"Get aggregated severity for alarm [${key.value}]")

      val activeAlarms: List[MetadataKey] = await(getActiveAlarmKeys(key))
      val severityKeys: List[SeverityKey] = activeAlarms.map(a => SeverityKey.fromAlarmKey(a))
      val severityValues                  = await(severityApi.mget(severityKeys))
      val severityList                    = severityValues.map(_.value)
      aggregratorByMax(severityList)
    }

  final override def subscribeAggregatedSeverityCallback(key: Key, callback: FullAlarmSeverity => Unit): AlarmSubscription = {
    log.debug(s"Subscribe aggregated severity for alarm [${key.value}] with a callback")
    subscribeAggregatedSeverity(key).to(Sink.foreach(callback)).run()
  }

  final override def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[FullAlarmSeverity]): AlarmSubscription = {
    log.debug(s"Subscribe aggregated severity for alarm [${key.value}] with an actor")
    subscribeAggregatedSeverityCallback(key, actorRef ! _)
  }

  final override def getCurrentSeverity(alarmKey: AlarmKey): Future[FullAlarmSeverity] =
    async {
      log.debug(s"Getting severity for alarm [${alarmKey.value}]")

      if (await(metadataApi.exists(alarmKey))) await(severityApi.get(alarmKey)).getOrElse(Disconnected)
      else logAndThrow(KeyNotFoundException(alarmKey))
    }

  private[alarm] def setCurrentSeverity(alarmKey: AlarmKey, severity: AlarmSeverity): Future[Done] =
    async {
      log.debug(
        s"Setting severity [${severity.name}] for alarm [${alarmKey.value}] with expire timeout [${settings.severityTTLInSeconds}] seconds"
      )

      // get alarm metadata
      val alarm = await(getMetadata(alarmKey))

      // validate if the provided severity is supported by this alarm
      if (!alarm.allSupportedSeverities.contains(severity))
        logAndThrow(InvalidSeverityException(alarmKey, alarm.allSupportedSeverities, severity))

      // set the severity of the alarm so that it does not transition to `Disconnected` state
      log.info(s"Updating current severity [${severity.name}] in alarm store")
      await(severityApi.setex(alarmKey, settings.severityTTLInSeconds, severity))
    }

  // PatternMessage gives three values:
  // pattern: e.g  __keyspace@0__:status.nfiraos.*.*,
  // channel: e.g. __keyspace@0__:status.nfiraos.trombone.tromboneAxisLowLimitAlarm,
  // message: event type as value: e.g. set, expire, expired
  private[alarm] def subscribeAggregatedSeverity(key: Key): Source[FullAlarmSeverity, AlarmSubscription] = {
    import AlarmRomaineCodec._

    // create new connection for every client
    val keySpaceApi = redisKeySpaceApi(severityApi)

    val severitySourceF: Future[Source[FullAlarmSeverity, RedisSubscription]] = async {
      val metadataKeys                          = await(getActiveAlarmKeys(key))
      val activeSeverityKeys: List[SeverityKey] = metadataKeys.map(a => SeverityKey.fromAlarmKey(a))
      val currentSeverities = await(severityApi.mget(activeSeverityKeys)).map(result => result.key -> result.value).toMap

      keySpaceApi
        .watchKeyspaceValue(activeSeverityKeys, OverflowStrategy.LATEST)
        .scan(currentSeverities) { case (data, RedisResult(severityKey, mayBeSeverity)) =>
          data + (severityKey -> mayBeSeverity)
        }
        .map(data => aggregratorByMax(data.values))
        .distinctUntilChanged
    }

    Source
      .futureSource(severitySourceF)
      .mapMaterializedValue { mat =>
        new AlarmSubscription {
          override def unsubscribe(): Future[Done] = mat.flatMap(_.unsubscribe())
          override def ready(): Future[Done]       = mat.flatMap(_.ready())
        }
      }
  }

  private def getActiveAlarmKeys(key: Key): Future[List[MetadataKey]] =
    async {
      val metadataKeys = await(metadataApi.keys(key))
      if (metadataKeys.isEmpty) logAndThrow(KeyNotFoundException(key))

      val keys = await(metadataApi.mget(metadataKeys)).collect {
        case RedisResult(metadataKey, Some(metadata)) if metadata.isActive => metadataKey
      }

      val result: List[MetadataKey] = if (keys.isEmpty) logAndThrow(InactiveAlarmException(key)) else keys
      result
    }

  private def aggregratorByMax(iterable: Iterable[Option[FullAlarmSeverity]]): FullAlarmSeverity =
    iterable.map(x => if (x.isEmpty) Disconnected else x.get).maxBy(_.level)

  private def logAndThrow(runtimeException: RuntimeException) = {
    log.error(runtimeException.getMessage, ex = runtimeException)
    throw runtimeException
  }
}
