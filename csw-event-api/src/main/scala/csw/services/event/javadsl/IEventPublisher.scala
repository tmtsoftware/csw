package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.{BiConsumer, Supplier}

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.scaladsl.EventPublisher

import scala.compat.java8.FunctionConverters.{enrichAsScalaFromBiConsumer, enrichAsScalaFromSupplier}
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.FiniteDuration

abstract class IEventPublisher(eventPublisher: EventPublisher) {
  def publish(event: Event): CompletableFuture[Done] = eventPublisher.publish(event).toJava.toCompletableFuture

  def publish[Mat](source: Source[Event, Mat]): Mat = eventPublisher.publish(source.asScala)

  def publish[Mat](source: Source[Event, Mat], onError: BiConsumer[Event, Throwable]): Mat =
    eventPublisher.publish(source.asScala, onError.asScala)

  def publish(eventGenerator: Supplier[Event], every: FiniteDuration): Cancellable =
    eventPublisher.publish(eventGenerator.asScala.apply(), every)

  def publish(eventGenerator: Supplier[Event], every: FiniteDuration, onError: BiConsumer[Event, Throwable]): Cancellable =
    eventPublisher.publish(eventGenerator.asScala.apply(), every, onError.asScala)

  def shutdown(): CompletableFuture[Done] = eventPublisher.shutdown().toJava.toCompletableFuture

  def asScala: EventPublisher = eventPublisher
}
