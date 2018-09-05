package csw.services.alarm.client.internal.redis

import csw.services.alarm.api.internal._
import csw.services.alarm.api.models._
import csw.services.alarm.client.internal.commons.serviceresolver.AlarmServiceResolver
import io.lettuce.core.RedisURI
import romaine.RomaineFactory
import romaine.async.RedisAsyncApi
import romaine.codec.RomaineStringCodec
import romaine.keyspace.RedisKeySpaceApi
import romaine.reactive.RedisSubscriptionApi

import scala.concurrent.ExecutionContext

class RedisConnectionsFactory(alarmServiceResolver: AlarmServiceResolver, masterId: String, romaineFactory: RomaineFactory)(
    implicit val ec: ExecutionContext
) {
  import csw.services.alarm.client.internal.AlarmCodec._
  import romaine.codec.RomaineStringCodec.stringRomaineCodec

  lazy val metadataApi: RedisAsyncApi[MetadataKey, AlarmMetadata]                   = asyncApi
  lazy val severityApi: RedisAsyncApi[SeverityKey, FullAlarmSeverity]               = asyncApi
  lazy val latchedSeverityApi: RedisAsyncApi[LatchedSeverityKey, FullAlarmSeverity] = asyncApi
  lazy val alarmTimeApi: RedisAsyncApi[AlarmTimeKey, AlarmTime]                     = asyncApi
  lazy val ackStatusApi: RedisAsyncApi[AckStatusKey, AcknowledgementStatus]         = asyncApi
  lazy val shelveStatusApi: RedisAsyncApi[ShelveStatusKey, ShelveStatus]            = asyncApi

  def asyncApi[K: RomaineStringCodec, V: RomaineStringCodec]: RedisAsyncApi[K, V] = romaineFactory.redisAsyncApi[K, V](redisURI)

  def subscriptionApi[K: RomaineStringCodec, V: RomaineStringCodec]: RedisSubscriptionApi[K, V] =
    romaineFactory.redisSubscriptionApi[K, V](redisURI)

  def redisKeySpaceApi[K: RomaineStringCodec, V: RomaineStringCodec](asyncApi: RedisAsyncApi[K, V]): RedisKeySpaceApi[K, V] =
    new RedisKeySpaceApi(subscriptionApi, asyncApi)

  private def redisURI = alarmServiceResolver.uri().map { uri ⇒
    RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build()
  }
}
