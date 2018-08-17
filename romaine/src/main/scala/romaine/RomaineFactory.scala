package romaine

import io.lettuce.core.{RedisClient, RedisURI}
import romaine.async.RedisAsyncScalaApi
import romaine.codec.{RomaineByteCodec, RomaineRedisCodec}
import romaine.reactive.{RedisSubscribeScalaApi, RedisSubscriptionApi}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RomaineFactory(redisClient: RedisClient)(implicit val ec: ExecutionContext) {
  def redisAsyncScalaApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): Future[RedisAsyncScalaApi[K, V]] = {
    redisClient
      .connectAsync(new RomaineRedisCodec[K, V], redisURI)
      .toScala
      .map(connection => new RedisAsyncScalaApi(connection.async()))
  }

  def redisSubscriptionApi[K: RomaineByteCodec, V: RomaineByteCodec](redisURI: RedisURI): Future[RedisSubscriptionApi[K, V]] = {
    redisClient
      .connectPubSubAsync(new RomaineRedisCodec[K, V], redisURI)
      .toScala
      .map(connection => new RedisSubscriptionApi(() => new RedisSubscribeScalaApi(connection.reactive())))
  }
}
