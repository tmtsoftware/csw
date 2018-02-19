package csw.services.event.internal.redis

import csw.services.event.internal.api.{EventSubscriberDriver, EventSubscriberDriverFactory}
import io.lettuce.core.{RedisClient, RedisURI}

import scala.concurrent.ExecutionContext

class RedisEventSubscriberDriverFactory(redisClient: RedisClient, redisURI: RedisURI)(implicit val ec: ExecutionContext)
    extends EventSubscriberDriverFactory {

  override def make(): EventSubscriberDriver = new RedisEventSubscriberDriver(redisClient, redisURI)
}
