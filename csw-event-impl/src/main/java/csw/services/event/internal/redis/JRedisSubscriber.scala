package csw.services.event.internal.redis

import acyclic.skipped
import csw.services.event.javadsl.IEventSubscriber

class JRedisSubscriber(redisSubscriber: RedisSubscriber) extends IEventSubscriber(redisSubscriber)
