package csw.services.event.internal.redis

import csw.services.event.internal.pubsub.JBaseEventPublisher

class JRedisPublisher(redisPublisher: RedisPublisher) extends JBaseEventPublisher(redisPublisher)
