package csw.services.event.perf

import akka.actor.ActorSystem
import com.typesafe.config.Config
import csw.services.event.internal.commons.Wiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory, RedisFactory}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

class TestWiring(actorSystem: ActorSystem) extends MockitoSugar {

  lazy val config: Config = actorSystem.settings.config
  lazy val wiring: Wiring = new Wiring(actorSystem)

  lazy val redisEnabled: Boolean = config.getBoolean("csw.test.EventThroughputSpec.redis-enabled")

  //################### Redis Configuration ###################
  lazy val redisHost: String          = config.getString("csw.test.EventThroughputSpec.redis.host")
  lazy val redisPort: Int             = config.getInt("csw.test.EventThroughputSpec.redis.port")
  lazy val redisClient: RedisClient   = RedisClient.create()
  lazy val redisFactory: RedisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)

  //################### Kafka Configuration ###################
  lazy val kafkaHost: String          = config.getString("csw.test.EventThroughputSpec.kafka.host")
  lazy val kafkaPort: Int             = config.getInt("csw.test.EventThroughputSpec.kafka.port")
  lazy val kafkaFactory: KafkaFactory = new KafkaFactory(mock[LocationService], wiring)

  def publisher: EventPublisher =
    if (redisEnabled) redisFactory.publisher(redisHost, redisPort)
    else kafkaFactory.publisher(kafkaHost, kafkaPort)

  def subscriber: EventSubscriber =
    if (redisEnabled) redisFactory.subscriber(redisHost, redisPort)
    else kafkaFactory.subscriber(kafkaHost, kafkaPort)

}
