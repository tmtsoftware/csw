package csw.services.alarm.client.internal.redis

import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus}
import csw.services.alarm.client.internal.AlarmCodec
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec, StringCodec}
import csw.services.alarm.client.internal.commons.serviceresolver.AlarmServiceResolver
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import romaine._
import romaine.reactive.{RedisKeySpaceApi, RedisKeySpaceCodec, RedisPSubscribeScalaApi}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisConnectionsFactory(redisClient: RedisClient, alarmServiceResolver: AlarmServiceResolver, masterId: String)(
    implicit val ec: ExecutionContext
) {

  lazy val metadataApiF: Future[RedisAsyncScalaApi[MetadataKey, AlarmMetadata]] = wrappedAsyncConnection(MetadataCodec)
  lazy val severityApiF: Future[RedisAsyncScalaApi[SeverityKey, AlarmSeverity]] = wrappedAsyncConnection(SeverityCodec)
  lazy val statusApiF: Future[RedisAsyncScalaApi[StatusKey, AlarmStatus]]       = wrappedAsyncConnection(StatusCodec)

  def asyncConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisAsyncCommands[K, V]] =
    redisURI.flatMap(redisUri ⇒ redisClient.connectAsync(alarmCodec, redisUri).toScala.map(_.async()))

  def wrappedAsyncConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisAsyncScalaApi[K, V]] =
    asyncConnection(alarmCodec).map(new RedisAsyncScalaApi(_))

  def reactiveConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisPubSubReactiveCommands[K, V]] =
    redisURI.flatMap(redisUri ⇒ redisClient.connectPubSubAsync(alarmCodec, redisUri).toScala.map(_.reactive()))

  def wrappedReactiveConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisPSubscribeScalaApi[K, V]] =
    reactiveConnection(alarmCodec).map(new RedisPSubscribeScalaApi(_))

  def redisKeySpaceApi[K, V](
      redisAsyncScalaApi: RedisAsyncScalaApi[K, V]
  )(implicit redisKeySpaceCodec: RedisKeySpaceCodec[K, V]): Future[RedisKeySpaceApi[K, V]] =
    wrappedReactiveConnection(StringCodec)
      .map(redisReactiveScalaApi ⇒ new RedisKeySpaceApi(redisReactiveScalaApi, redisAsyncScalaApi))

  private def redisURI =
    alarmServiceResolver
      .uri()
      .map { uri ⇒
        RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build()
      }
}
