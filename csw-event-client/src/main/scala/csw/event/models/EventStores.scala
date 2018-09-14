package csw.event.models

import io.lettuce.core.RedisClient

sealed trait EventStore

/**
 * Event service supports two underlying implementations for event store
 * 1. [[csw.event.models.EventStores.RedisStore]]: This is the default and recommended store. If you are creating [[csw.event.api.scaladsl.EventService]] using [[csw.event.EventServiceFactory]], then you should shutdown redis client when it is no longer in use.
 * 2. [[csw.event.models.EventStores.KafkaStore]]: This can be used to create an [[csw.event.api.scaladsl.EventService]] which is backed by Kafka event store. You should not use this unless you have strong reasons to do so.
 *
 * @note If you are using csw-framework, your component will already have an event service injected which is backed up by [[csw.event.models.EventStores.RedisStore]]. You do not need to take the [[csw.event.EventServiceFactory]] route.
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
