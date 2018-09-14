package csw.event.internal.commons.javawrappers

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.{Consumer, Supplier}

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.params.events.Event
import csw.event.api.exceptions.PublishFailure
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.scaladsl.EventPublisher

import scala.compat.java8.DurationConverters.DurationOps
import scala.compat.java8.FunctionConverters.{enrichAsScalaFromConsumer, enrichAsScalaFromSupplier}
import scala.compat.java8.FutureConverters.FutureOps

/**
 * Java API for [[csw.event.api.scaladsl.EventPublisher]]
 */
class JEventPublisher(eventPublisher: EventPublisher) extends IEventPublisher {
  override def publish(event: Event): CompletableFuture[Done] = eventPublisher.publish(event).toJava.toCompletableFuture

  override def publish[Mat](source: Source[Event, Mat]): Mat = eventPublisher.publish(source.asScala)

  override def publish[Mat](source: Source[Event, Mat], onError: Consumer[PublishFailure]): Mat =
    eventPublisher.publish(source.asScala, onError.asScala)

  override def publish(eventGenerator: Supplier[Event], every: Duration): Cancellable =
    eventPublisher.publish(eventGenerator.asScala.apply(), every.toScala)

  override def publish(eventGenerator: Supplier[Event], every: Duration, onError: Consumer[PublishFailure]): Cancellable =
    eventPublisher.publish(eventGenerator.asScala.apply(), every.toScala, onError.asScala)

  override def shutdown(): CompletableFuture[Done] = eventPublisher.shutdown().toJava.toCompletableFuture

  def asScala: EventPublisher = eventPublisher
}
