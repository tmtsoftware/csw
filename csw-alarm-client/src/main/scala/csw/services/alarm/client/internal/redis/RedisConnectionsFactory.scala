package csw.services.alarm.client.internal.redis

import csw.services.alarm.client.internal.AlarmCodec
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import romaine.{RedisAsyncScalaApi, RedisReactiveScalaApi}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisConnectionsFactory(redisClient: RedisClient, redisURI: RedisURI)(implicit val ec: ExecutionContext) {

  def asyncConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisAsyncCommands[K, V]] =
    redisClient.connectAsync(alarmCodec, redisURI).toScala.map(_.async())

  def reactiveConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisPubSubReactiveCommands[K, V]] =
    redisClient.connectPubSubAsync(alarmCodec, redisURI).toScala.map(_.reactive())

  def wrappedAsyncConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisAsyncScalaApi[K, V]] =
    asyncConnection(alarmCodec).map(new RedisAsyncScalaApi(_))

  def wrappedReactiveConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisReactiveScalaApi[K, V]] =
    reactiveConnection(alarmCodec).map(new RedisReactiveScalaApi(_))
}
