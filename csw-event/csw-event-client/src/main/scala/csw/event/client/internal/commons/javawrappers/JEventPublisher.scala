/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.commons.javawrappers

import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.{Consumer, Supplier}

import org.apache.pekko.Done
import org.apache.pekko.actor.Cancellable
import org.apache.pekko.stream.javadsl.Source
import csw.event.api.exceptions.PublishFailure
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.scaladsl.EventPublisher
import csw.params.events.Event
import csw.params.extensions.OptionConverters.RichOptional
import csw.time.core.models.TMTTime

import scala.jdk.DurationConverters.*
import scala.jdk.FunctionConverters.*
import scala.jdk.FutureConverters.*

/**
 * Java API for [[csw.event.api.scaladsl.EventPublisher]]
 */
private[event] class JEventPublisher(eventPublisher: EventPublisher) extends IEventPublisher {
  override def publish(event: Event): CompletableFuture[Done] = eventPublisher.publish(event).asJava.toCompletableFuture

  override def publish[Mat](source: Source[Event, Mat]): Mat = eventPublisher.publish(source.asScala)

  override def publish[Mat](source: Source[Event, Mat], onError: Consumer[PublishFailure]): Mat =
    eventPublisher.publish(source.asScala, onError.asScala)

  override def publish(eventGenerator: Supplier[Optional[Event]], every: Duration): Cancellable =
    eventPublisher.publish(eventGenerator.get().asScala, every.toScala)

  override def publish(eventGenerator: Supplier[Optional[Event]], startTime: TMTTime, every: Duration): Cancellable =
    eventPublisher.publish(eventGenerator.get().asScala, startTime, every.toScala)

  override def publish(
      eventGenerator: Supplier[Optional[Event]],
      every: Duration,
      onError: Consumer[PublishFailure]
  ): Cancellable =
    eventPublisher.publish(eventGenerator.get().asScala, every.toScala, onError.asScala)

  override def publish(
      eventGenerator: Supplier[Optional[Event]],
      startTime: TMTTime,
      every: Duration,
      onError: Consumer[PublishFailure]
  ): Cancellable =
    eventPublisher.publish(eventGenerator.get().asScala, startTime, every.toScala, onError.asScala)

  override def publishAsync(eventGenerator: Supplier[CompletableFuture[Optional[Event]]], every: Duration): Cancellable =
    eventPublisher.publishAsync(eventGenerator.get().thenApply[Option[Event]](_.asScala).asScala, every.toScala)

  override def publishAsync(
      eventGenerator: Supplier[CompletableFuture[Optional[Event]]],
      startTime: TMTTime,
      every: Duration
  ): Cancellable =
    eventPublisher.publishAsync(eventGenerator.get().thenApply[Option[Event]](_.asScala).asScala, startTime, every.toScala)

  override def publishAsync(
      eventGenerator: Supplier[CompletableFuture[Optional[Event]]],
      every: Duration,
      onError: Consumer[PublishFailure]
  ): Cancellable =
    eventPublisher.publishAsync(eventGenerator.get().thenApply[Option[Event]](_.asScala).asScala, every.toScala, onError.asScala)

  override def publishAsync(
      eventGenerator: Supplier[CompletableFuture[Optional[Event]]],
      startTime: TMTTime,
      every: Duration,
      onError: Consumer[PublishFailure]
  ): Cancellable =
    eventPublisher.publishAsync(
      eventGenerator.get().thenApply[Option[Event]](_.asScala).asScala,
      startTime,
      every.toScala,
      onError.asScala
    )

  override def shutdown(): CompletableFuture[Done] = eventPublisher.shutdown().asJava.toCompletableFuture

  def asScala: EventPublisher = eventPublisher

}
