package csw.services.event.scaladsl

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.Materializer
import csw.services.event.internal.kafka.{KafkaPublisher, KafkaSubscriber}
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.EventServiceResolver
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class KafkaFactory(
    eventServiceResolver: EventServiceResolver,
    eventPublisherUtil: EventPublisherUtil,
    eventSubscriberUtil: EventSubscriberUtil
)(implicit actorSystem: ActorSystem, ec: ExecutionContext, mat: Materializer) {

  def publisher(host: String, port: Int): EventPublisher = new KafkaPublisher(producerSettings(host, port), eventPublisherUtil)

  def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    publisher(uri.getHost, uri.getPort)
  }

  def subscriber(host: String, port: Int): EventSubscriber =
    new KafkaSubscriber(consumerSettings(host, port), eventSubscriberUtil)

  def subscriber(): Future[EventSubscriber] = async {
    val uri: URI = await(eventServiceResolver.uri)
    subscriber(uri.getHost, uri.getPort)
  }

  private def producerSettings(host: String, port: Int) =
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(s"$host:$port")

  private def consumerSettings(host: String, port: Int) =
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(s"$host:$port")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
      .withGroupId(UUID.randomUUID().toString)
}
