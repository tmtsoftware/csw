package csw.services.alarm.client.internal.redis

import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.models.{AlarmMetadata, AlarmStatus, FullAlarmSeverity}
import csw.services.alarm.client.internal.commons.serviceresolver.AlarmServiceResolver
import io.lettuce.core.RedisURI
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec
import romaine.reactive.{RedisKeySpaceApi, RedisSubscriptionApi}

import scala.concurrent.{ExecutionContext, Future}

class RedisConnectionsFactory(alarmServiceResolver: AlarmServiceResolver, masterId: String, romaineFactory: RomaineFactory)(
    implicit val ec: ExecutionContext
) {
  import csw.services.alarm.client.internal.AlarmCodec._
  import romaine.codec.RomaineStringCodec.stringRomaineCodec

  implicit val metadataRomainCodec: RomaineStringCodec[AlarmMetadata]     = viaJsonCodec
  implicit val severityRomainCodec: RomaineStringCodec[FullAlarmSeverity] = viaJsonCodec
  implicit val statusRomainCodec: RomaineStringCodec[AlarmStatus]         = viaJsonCodec

  lazy val metadataApiF: Future[RedisAsyncApi[MetadataKey, AlarmMetadata]]     = asyncApi
  lazy val severityApiF: Future[RedisAsyncApi[SeverityKey, FullAlarmSeverity]] = asyncApi
  lazy val statusApiF: Future[RedisAsyncApi[StatusKey, AlarmStatus]]           = asyncApi

  def asyncApi[K: RomaineStringCodec, V: RomaineStringCodec]: Future[RedisAsyncApi[K, V]] =
    redisURI.flatMap(redisURI => romaineFactory.redisAsyncApi[K, V](redisURI))

  def subscriptionApi[K: RomaineStringCodec, V: RomaineStringCodec]: RedisSubscriptionApi[K, V] =
    romaineFactory.redisSubscriptionApi[K, V](redisURI)

  def redisKeySpaceApi[K: RomaineStringCodec, V: RomaineStringCodec](asyncApi: RedisAsyncApi[K, V]): RedisKeySpaceApi[K, V] =
    new RedisKeySpaceApi(subscriptionApi, asyncApi)

  private def redisURI =
    alarmServiceResolver
      .uri()
      .map { uri ⇒
        RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build()
      }
}
