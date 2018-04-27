package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.{BiConsumer, Supplier}

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailed
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.duration.FiniteDuration

trait IEventPublisher {

  def publish(event: Event): CompletableFuture[Done]

  def publish[Mat](source: Source[Event, Mat]): Mat

  def publish[Mat](source: Source[Event, Mat], onError: BiConsumer[Event, PublishFailed]): Mat

  def publish(eventGenerator: Supplier[Event], every: FiniteDuration): Cancellable

  def publish(eventGenerator: Supplier[Event], every: FiniteDuration, onError: BiConsumer[Event, PublishFailed]): Cancellable

  def shutdown(): CompletableFuture[Done]

  def asScala: EventPublisher
}
