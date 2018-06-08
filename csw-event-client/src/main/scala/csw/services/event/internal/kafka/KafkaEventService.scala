package csw.services.event.internal.kafka

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.Materializer
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class KafkaEventService(eventServiceResolver: EventServiceResolver)(
    implicit actorSystem: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer
) extends EventService {

  override val defaultPublisher: Future[EventPublisher]   = publisher()
  override val defaultSubscriber: Future[EventSubscriber] = subscriber()

  override def makeNewPublisher(): Future[EventPublisher] = publisher()

  private[csw] def publisher(host: String, port: Int): EventPublisher =
    new KafkaPublisher(producerSettings(host, port))

  private def publisher(): Future[EventPublisher] = async {
    val uri: URI = await(eventServiceResolver.uri)
    publisher(uri.getHost, uri.getPort)
  }

  private[csw] def subscriber(host: String, port: Int): EventSubscriber =
    new KafkaSubscriber(consumerSettings(host, port))

  private def subscriber(): Future[EventSubscriber] = async {
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
