package csw.services.event.internal.redis

import csw.services.event.javadsl.IEventSubscriber

class JRedisSubscriber(redisSubscriber: RedisSubscriber) extends IEventSubscriber(redisSubscriber)
