package csw.services.event.javadsl

import java.net.URI
import java.util.concurrent.CompletableFuture

import akka.kafka.{ConsumerSettings, ProducerSettings}
import csw.services.event.internal.kafka.{JKafkaPublisher, JKafkaSubscriber, KafkaPublisher, KafkaSubscriber}
import csw.services.event.internal.wiring.{EventServiceResolver, Wiring}
import csw.services.location.scaladsl.LocationService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters.FutureOps

class JKafkaFactory(locationService: LocationService, wiring: Wiring) {
  import wiring._

  def publisher(host: String, port: Int): IEventPublisher = {
    val kafkaPublisher = new KafkaPublisher(producerSettings(host, port))
    new JKafkaPublisher(kafkaPublisher)
  }

  def publisher(): CompletableFuture[IEventPublisher] =
    async {
      val uri: URI = await(eventServiceResolver.uri)
      publisher(uri.getHost, uri.getPort)
    }.toJava.toCompletableFuture

  def subscriber(host: String, port: Int): IEventSubscriber = {
    val kafkaSubscriber = new KafkaSubscriber(consumerSettings(host, port))
    new JKafkaSubscriber(kafkaSubscriber)
  }

  def subscriber(): CompletableFuture[IEventSubscriber] =
    async {
      val uri: URI = await(eventServiceResolver.uri)
      subscriber(uri.getHost, uri.getPort)
    }.toJava.toCompletableFuture

  private def consumerSettings(host: String, port: Int) =
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      .withBootstrapServers(s"$host:$port")

  private def producerSettings(host: String, port: Int) =
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(s"$host:$port")

  private val eventServiceResolver = new EventServiceResolver(locationService)
}
