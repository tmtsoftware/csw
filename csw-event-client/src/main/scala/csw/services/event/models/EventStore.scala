package csw.services.event.models

import io.lettuce.core.RedisClient

sealed trait EventStore

object EventStore {
  case class RedisStore(redisClient: RedisClient = RedisClient.create()) extends EventStore
  case object KafkaStore                                                 extends EventStore
}
