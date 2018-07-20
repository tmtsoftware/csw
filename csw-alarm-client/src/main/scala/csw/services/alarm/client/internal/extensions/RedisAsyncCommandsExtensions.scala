package csw.services.alarm.client.internal.extensions

import csw.services.alarm.client.internal.RedisAsyncScalaApi
import io.lettuce.core.api.async.RedisAsyncCommands

import scala.concurrent.ExecutionContext

object RedisAsyncCommandsExtensions {
  implicit class RichRedisAsyncCommands[K, V](redisAsyncCommands: RedisAsyncCommands[K, V])(implicit ex: ExecutionContext) {
    def toScala: RedisAsyncScalaApi[K, V] = new RedisAsyncScalaApi(redisAsyncCommands)
  }
}
