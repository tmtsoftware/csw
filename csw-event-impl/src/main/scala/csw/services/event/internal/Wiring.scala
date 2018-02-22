package csw.services.event.internal

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.kafka.{KafkaPublisher, KafkaSubscriber}
import csw.services.event.internal.redis.{RedisPublisher, RedisSubscriber}
import io.lettuce.core.{RedisClient, RedisURI}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization._

import scala.concurrent.ExecutionContext

class Wiring(redisPort: Int = 6379) {

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  implicit lazy val ec: ExecutionContext     = actorSystem.dispatcher
  implicit lazy val mat: Materializer        = ActorMaterializer()

  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  lazy val resumingMat: Materializer = ActorMaterializer(settings)

  //Redis
  lazy val redisURI: RedisURI       = RedisURI.create("localhost", redisPort)
  lazy val redisClient: RedisClient = RedisClient.create(redisURI)
  lazy val redisPublisher           = new RedisPublisher(redisClient, redisURI)(ec, resumingMat)
  lazy val redisSubscriber          = new RedisSubscriber(redisClient, redisURI)(ec, resumingMat)

  //kafka
  lazy val producerSettings: ProducerSettings[String, Array[Byte]] =
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers("localhost:6001")

  lazy val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(actorSystem, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers("localhost:6001")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  lazy val kafkaPublisher  = new KafkaPublisher(producerSettings)(resumingMat)
  lazy val kafkaSubscriber = new KafkaSubscriber(consumerSettings)(resumingMat)
}
