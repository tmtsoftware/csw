package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.services.event.scaladsl.KafkaFactory
import csw.services.location.javadsl.ILocationService

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JKafkaFactory(locationService: ILocationService)(implicit val actorSystem: ActorSystem,
                                                       ec: ExecutionContext,
                                                       mat: Materializer) {

  private val kafkaFactory = new KafkaFactory(locationService.asScala)

  def publisher(host: String, port: Int): IEventPublisher = kafkaFactory.publisher(host, port).asJava
  def publisher(): CompletableFuture[IEventPublisher]     = kafkaFactory.publisher().map(_.asJava).toJava.toCompletableFuture

  def subscriber(host: String, port: Int): IEventSubscriber = kafkaFactory.subscriber(host, port).asJava
  def subscriber(): CompletableFuture[IEventSubscriber]     = kafkaFactory.subscriber().map(_.asJava).toJava.toCompletableFuture
}
