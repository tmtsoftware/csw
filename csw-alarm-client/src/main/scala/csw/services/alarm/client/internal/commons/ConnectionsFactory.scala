package csw.services.alarm.client.internal.commons
import csw.services.alarm.client.internal.AlarmCodec
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}
import romaine.{RedisAsyncScalaApi, RedisReactiveScalaApi}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class ConnectionsFactory(redisClient: RedisClient, redisURI: RedisURI)(implicit ec: ExecutionContext) {

  def asyncConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisAsyncCommands[K, V]] =
    redisClient.connectAsync(alarmCodec, redisURI).toScala.map(_.async())

  def reactiveConnection[K, V](codec: RedisCodec[K, V]): Future[RedisPubSubReactiveCommands[K, V]] =
    redisClient.connectPubSubAsync(codec, redisURI).toScala.map(_.reactive())

  def wrappedAsyncConnection[K, V](alarmCodec: AlarmCodec[K, V]): Future[RedisAsyncScalaApi[K, V]] =
    asyncConnection(alarmCodec).map(new RedisAsyncScalaApi(_))

  def wrappedReactiveConnection[K, V](codec: RedisCodec[K, V]): Future[RedisReactiveScalaApi[K, V]] =
    reactiveConnection(codec).map(new RedisReactiveScalaApi(_))
}
