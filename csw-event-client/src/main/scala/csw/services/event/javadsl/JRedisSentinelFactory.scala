package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import csw.services.event.internal.commons.EventServiceAdapter
import csw.services.event.internal.redis.{RedisPublisher, RedisSubscriber}
import csw.services.event.scaladsl.RedisSentinelFactory

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JRedisSentinelFactory(redisFactory: RedisSentinelFactory)(implicit ec: ExecutionContext) {

  def publisher(host: String, port: Int, masterId: String): IEventPublisher =
    EventServiceAdapter.asJava(redisFactory.publisher(host, port, masterId).asInstanceOf[RedisPublisher])

  def publisher(masterId: String): CompletableFuture[IEventPublisher] =
    redisFactory
      .publisher(masterId)
      .map(publisher ⇒ EventServiceAdapter.asJava(publisher.asInstanceOf[RedisPublisher]))
      .toJava
      .toCompletableFuture

  def subscriber(host: String, port: Int, masterId: String): IEventSubscriber =
    EventServiceAdapter.asJava(redisFactory.subscriber(host, port, masterId).asInstanceOf[RedisSubscriber])

  def subscriber(masterId: String): CompletableFuture[IEventSubscriber] =
    redisFactory
      .subscriber(masterId)
      .map(subscriber ⇒ EventServiceAdapter.asJava(subscriber.asInstanceOf[RedisSubscriber]))
      .toJava
      .toCompletableFuture
}
