//package csw.services.alarm.client.internal.extensions
//
//import csw.services.alarm.client.internal.RedisScalaApi
//import io.lettuce.core.api.async.RedisAsyncCommands
//import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
//
//import scala.concurrent.ExecutionContext
//
//object RedisCommandsExtensions {
//  implicit class RichRedisAsyncCommands[K, V](
//      redisAsyncCommands: RedisAsyncCommands[K, V],
//      redisReactiveCommands: RedisPubSubReactiveCommands[K, V]
//  )(implicit ex: ExecutionContext) {
//    def toScala: RedisScalaApi[K, V] = new RedisScalaApi(redisAsyncCommands, redisReactiveCommands)
//  }
//}
