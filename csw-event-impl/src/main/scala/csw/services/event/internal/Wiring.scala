package csw.services.event.internal

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.kafka.{KafkaPublisher, KafkaSubscriber}
import csw.services.event.internal.redis.{RedisGateway, RedisPublisher, RedisSubscriber}
import io.lettuce.core.RedisURI
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization._

import scala.concurrent.ExecutionContext

class Wiring(host: String = "localhost", redisPort: Int = 6379, kafkaPort: Int = 6001) {

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher
  implicit lazy val mat: Materializer        = ActorMaterializer()

  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  lazy val resumingMat: Materializer = ActorMaterializer(settings)

  //Redis
  lazy val redisURI: RedisURI = RedisURI.create(host, redisPort)
  lazy val redisGateway       = new RedisGateway(redisURI)
  lazy val redisPublisher     = new RedisPublisher(redisGateway)(ec, resumingMat)
  lazy val redisSubscriber    = new RedisSubscriber(redisGateway)(ec, resumingMat)

  //kafka
  lazy val producerSettings: ProducerSettings[String, Array[Byte]] =
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(s"$host:$kafkaPort")

  lazy val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(s"$host:$kafkaPort")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  lazy val kafkaPublisher  = new KafkaPublisher(producerSettings)(resumingMat)
  lazy val kafkaSubscriber = new KafkaSubscriber(consumerSettings)(resumingMat)
}
