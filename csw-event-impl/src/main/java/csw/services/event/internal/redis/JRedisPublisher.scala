package csw.services.event.internal.redis

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.javadsl.IEventPublisher
import csw.services.event.scaladsl.EventPublisher

import scala.compat.java8.FunctionConverters.enrichAsScalaFromSupplier
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.FiniteDuration

class JRedisPublisher(redisPublisher: RedisPublisher) extends IEventPublisher {
  override def publish[Mat](source: Source[Event, Mat]): Mat = redisPublisher.publish(source.asScala)

  override def publish(event: Event): CompletableFuture[Done] = redisPublisher.publish(event).toJava.toCompletableFuture

  override def publish(eventGenerator: Supplier[Event], every: FiniteDuration): Cancellable =
    redisPublisher.publish(eventGenerator.asScala, every)

  override def shutdown(): CompletableFuture[Done] = redisPublisher.shutdown().toJava.toCompletableFuture

  override def asScala: EventPublisher = redisPublisher
}
