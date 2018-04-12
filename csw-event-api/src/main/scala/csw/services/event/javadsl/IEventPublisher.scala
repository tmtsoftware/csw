package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.scaladsl.EventPublisher

import scala.compat.java8.FunctionConverters.enrichAsScalaFromSupplier
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.FiniteDuration

abstract class IEventPublisher(eventPublisher: EventPublisher) {
  def publish[Mat](source: Source[Event, Mat]): Mat = eventPublisher.publish(source.asScala)

  def publish(event: Event): CompletableFuture[Done] = eventPublisher.publish(event).toJava.toCompletableFuture

  def publish(eventGenerator: Supplier[Event], every: FiniteDuration): Cancellable =
    eventPublisher.publish(eventGenerator.asScala.apply(), every)

  def shutdown(): CompletableFuture[Done] = eventPublisher.shutdown().toJava.toCompletableFuture

  def asScala: EventPublisher = eventPublisher
}
