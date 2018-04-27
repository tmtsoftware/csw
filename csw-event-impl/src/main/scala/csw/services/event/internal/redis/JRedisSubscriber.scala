package csw.services.event.internal.redis

import csw.services.event.internal.pubsub.JBaseEventSubscriber

class JRedisSubscriber(redisSubscriber: RedisSubscriber) extends JBaseEventSubscriber(redisSubscriber)
