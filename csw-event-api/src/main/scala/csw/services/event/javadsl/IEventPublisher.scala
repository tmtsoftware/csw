package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.duration.FiniteDuration

trait IEventPublisher {
  def publish[Mat](source: Source[Event, Mat]): Mat

  def publish(event: Event): CompletableFuture[Done]

  def publish(eventGenerator: () => Event, every: FiniteDuration): Cancellable

  def shutdown(): CompletableFuture[Done]

  def asScala: EventPublisher
}
