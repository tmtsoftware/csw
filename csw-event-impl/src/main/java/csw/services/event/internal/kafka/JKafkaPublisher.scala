package csw.services.event.internal.kafka

import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.Cancellable
import akka.stream.javadsl.Source
import csw.messages.events.Event
import csw.services.event.javadsl.IEventPublisher
import csw.services.event.scaladsl.EventPublisher

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.FiniteDuration

class JKafkaPublisher(kafkaPublisher: KafkaPublisher) extends IEventPublisher {

  override def publish[Mat](source: Source[Event, Mat]): Mat = kafkaPublisher.publish(source.asScala)

  override def publish(event: Event): CompletableFuture[Done] = kafkaPublisher.publish(event).toJava.toCompletableFuture

  override def publish(eventGenerator: () ⇒ Event, every: FiniteDuration): Cancellable =
    kafkaPublisher.publish(eventGenerator, every)

  override def shutdown(): CompletableFuture[Done] = kafkaPublisher.shutdown().toJava.toCompletableFuture

  override def asScala: EventPublisher = kafkaPublisher
}
