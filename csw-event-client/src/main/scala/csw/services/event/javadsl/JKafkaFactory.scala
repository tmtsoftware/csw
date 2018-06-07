package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.ActorSystem
import csw.services.event.internal.commons.EventServiceAdapter
import csw.services.event.scaladsl.KafkaFactory

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JKafkaFactory(kafkaFactory: KafkaFactory)(implicit val actorSystem: ActorSystem, ec: ExecutionContext) {

  def publisher(host: String, port: Int): IEventPublisher =
    EventServiceAdapter.asJava(kafkaFactory.publisher(host, port))

  def publisher(): CompletableFuture[IEventPublisher] =
    kafkaFactory
      .publisher()
      .map(EventServiceAdapter.asJava)
      .toJava
      .toCompletableFuture

  def subscriber(host: String, port: Int): IEventSubscriber =
    EventServiceAdapter.asJava(kafkaFactory.subscriber(host, port))

  def subscriber(): CompletableFuture[IEventSubscriber] =
    kafkaFactory
      .subscriber()
      .map(EventServiceAdapter.asJava)
      .toJava
      .toCompletableFuture
}
