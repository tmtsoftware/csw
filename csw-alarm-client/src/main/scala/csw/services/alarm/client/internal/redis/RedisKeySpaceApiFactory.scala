package csw.services.alarm.client.internal.redis

import csw.services.alarm.client.internal.AlarmCodec.StringCodec
import romaine.{RedisAsyncScalaApi, RedisKeySpaceApi, RedisKeySpaceCodec}

import scala.concurrent.{ExecutionContext, Future}

class RedisKeySpaceApiFactory(connectionsFactory: RedisConnectionsFactory) {

  implicit val ec: ExecutionContext = connectionsFactory.ec

  def make[K, V](
      redisAsyncScalaApi: RedisAsyncScalaApi[K, V]
  )(implicit redisKeySpaceCodec: RedisKeySpaceCodec[K, V]): Future[RedisKeySpaceApi[K, V]] =
    connectionsFactory
      .wrappedReactiveConnection(StringCodec)
      .map(redisReactiveScalaApi ⇒ new RedisKeySpaceApi(redisReactiveScalaApi, redisAsyncScalaApi))

}
