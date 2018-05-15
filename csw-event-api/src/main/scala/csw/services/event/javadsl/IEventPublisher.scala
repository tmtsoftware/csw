package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture
import java.util.function.{Consumer, Supplier}

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event

import scala.concurrent.duration.FiniteDuration

trait IEventPublisher {

  def publish(event: Event): CompletableFuture[Done]

  def publish[Mat](source: Source[Event, Mat]): Mat

  def publish[Mat](source: Source[Event, Mat], onError: Consumer[Event]): Mat

  def publish(eventGenerator: Supplier[Event], every: FiniteDuration): Cancellable

  def publish(eventGenerator: Supplier[Event], every: FiniteDuration, onError: Consumer[Event]): Cancellable

  def shutdown(): CompletableFuture[Done]
}
