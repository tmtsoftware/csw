package csw.services.event.internal.kafka

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.Materializer
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.concurrent.ExecutionContext

class KafkaEventService(host: String, port: Int)(
    implicit actorSystem: ActorSystem,
    ec: ExecutionContext,
    mat: Materializer
) extends EventService {

  override val defaultPublisher: EventPublisher   = publisher()
  override val defaultSubscriber: EventSubscriber = subscriber()

  override def makeNewPublisher(): EventPublisher = publisher()

  private lazy val producerSettings =
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(s"$host:$port")

  private lazy val consumerSettings =
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(s"$host:$port")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
      .withGroupId(UUID.randomUUID().toString)

  private[csw] def publisher(): EventPublisher   = new KafkaPublisher(producerSettings)
  private[csw] def subscriber(): EventSubscriber = new KafkaSubscriber(consumerSettings)
}
