package csw.services.event.internal.kafka

import java.net.URI

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import csw.services.event.internal.commons.Wiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization._

class KafkaWiring(host: String, port: Int, actorSystem: ActorSystem) extends Wiring(host, port, actorSystem) {

  lazy val producerSettings: ProducerSettings[String, Array[Byte]] =
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(s"$host:$port")

  lazy val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(s"$host:$port")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  override def publisher(): EventPublisher = new KafkaPublisher(producerSettings)

  override def subscriber(): EventSubscriber = new KafkaSubscriber(consumerSettings)

}

object KafkaWiring {
  def apply(uri: URI, actorSystem: ActorSystem): KafkaWiring = new KafkaWiring(uri.getHost, uri.getPort, actorSystem)
}
