package csw.services.event.internal.kafka

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.Materializer
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.EventService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.concurrent.{ExecutionContext, Future}

class KafkaEventService(eventServiceResolver: EventServiceResolver)(
    implicit actorSystem: ActorSystem,
    val executionContext: ExecutionContext,
    mat: Materializer
) extends EventService {

  override val defaultPublisher: Future[KafkaPublisher]   = publisher()
  override val defaultSubscriber: Future[KafkaSubscriber] = subscriber()

  override def makeNewPublisher(): Future[KafkaPublisher] = publisher()

  private lazy val producerSettings = eventServiceResolver.uri.map { uri ⇒
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
  }

  private lazy val consumerSettings = eventServiceResolver.uri.map { uri ⇒
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
      .withGroupId(UUID.randomUUID().toString)
  }

  private[csw] def publisher(): Future[KafkaPublisher]   = producerSettings.map(new KafkaPublisher(_))
  private[csw] def subscriber(): Future[KafkaSubscriber] = consumerSettings.map(new KafkaSubscriber(_))
}
