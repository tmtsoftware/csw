package csw.services.alarm.client.internal.redis

import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus}
import csw.services.alarm.client.internal.commons.serviceresolver.AlarmServiceResolver
import io.lettuce.core.RedisURI
import romaine.RomaineFactory
import romaine.async.RedisAsyncScalaApi
import romaine.codec.RomaineStringCodec
import romaine.reactive.{RedisKeySpaceApi, RedisSubscriptionApi}

import scala.concurrent.{ExecutionContext, Future}

class RedisConnectionsFactory(alarmServiceResolver: AlarmServiceResolver, masterId: String, romaineFactory: RomaineFactory)(
    implicit val ec: ExecutionContext
) {
  import csw.services.alarm.client.internal.AlarmCodec._

  lazy val metadataApiF: Future[RedisAsyncScalaApi[MetadataKey, AlarmMetadata]] = asyncApi
  lazy val severityApiF: Future[RedisAsyncScalaApi[SeverityKey, AlarmSeverity]] = asyncApi
  lazy val statusApiF: Future[RedisAsyncScalaApi[StatusKey, AlarmStatus]]       = asyncApi

  def asyncApi[K: RomaineStringCodec, V: RomaineStringCodec]: Future[RedisAsyncScalaApi[K, V]] =
    redisURI.flatMap(x => romaineFactory.redisAsyncScalaApi[K, V](x))

  def subscriptionApi[K: RomaineStringCodec, V: RomaineStringCodec]: Future[RedisSubscriptionApi[K, V]] =
    redisURI.flatMap(x => romaineFactory.redisSubscriptionApi[K, V](x))

  def redisKeySpaceApi[K: RomaineStringCodec, V: RomaineStringCodec](
      asyncApi: RedisAsyncScalaApi[K, V]
  ): Future[RedisKeySpaceApi[K, V]] =
    subscriptionApi[String, String].map(x => new RedisKeySpaceApi(x, asyncApi))

  private def redisURI =
    alarmServiceResolver
      .uri()
      .map { uri ⇒
        RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build()
      }
}
