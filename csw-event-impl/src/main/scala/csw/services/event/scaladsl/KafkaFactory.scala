package csw.services.event.scaladsl

import java.net.URI

import akka.kafka.{ConsumerSettings, ProducerSettings}
import csw.services.event.internal.kafka.{KafkaPublisher, KafkaSubscriber}
import csw.services.event.internal.wiring.{EventServiceResolver, Wiring}
import csw.services.location.scaladsl.LocationService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.async.Async.{async, await}
import scala.concurrent.Future

class KafkaFactory(locationService: LocationService, wiring: Wiring) {
  import wiring._

  private val eventServiceResolver = new EventServiceResolver(locationService)

  def publisher(host: String, port: Int): EventPublisher = new KafkaPublisher(producerSettings(host, port))

  def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    publisher(uri.getHost, uri.getPort)
  }

  def subscriber(host: String, port: Int): EventSubscriber = new KafkaSubscriber(consumerSettings(host, port))

  def subscriber(): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    subscriber(uri.getHost, uri.getPort)
  }

  private def producerSettings(host: String, port: Int) =
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(s"$host:$port")

  private def consumerSettings(host: String, port: Int) =
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      .withBootstrapServers(s"$host:$port")
}
