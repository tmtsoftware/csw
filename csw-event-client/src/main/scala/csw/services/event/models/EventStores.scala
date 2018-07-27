package csw.services.event.models

import io.lettuce.core.RedisClient

sealed trait EventStore

/**
 * Event service supports two underlying implementations for event store
 * 1. [[csw.services.event.models.EventStores.RedisStore]]: This is the default and recommended store. If you are creating [[csw.services.event.api.scaladsl.EventService]] using [[csw.services.event.EventServiceFactory]], then you should shutdown redis client when it is no longer in use.
 * 2. [[csw.services.event.models.EventStores.KafkaStore]]: This can be used to create an [[csw.services.event.api.scaladsl.EventService]] which is backed by Kafka event store. You should not use this unless you have strong reasons to do so.
 *
 * @note If you are using csw-framework, your component will already have an event service injected which is backed up by [[csw.services.event.models.EventStores.RedisStore]]. You do not need to take the [[csw.services.event.EventServiceFactory]] route.
 */
object EventStores {
  case class RedisStore(redisClient: RedisClient = RedisClient.create()) extends EventStore
  case object KafkaStore                                                 extends EventStore

  /**
   * Java helpers to select appropriate event store.
   */
  val jRedisStore: EventStore = RedisStore()
  val jKafkaStore: EventStore = KafkaStore
}
