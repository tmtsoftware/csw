package csw.services.event.perf

import akka.actor.ActorSystem
import com.typesafe.config.Config
import csw.services.event.internal.commons.Wiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory, RedisFactory}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

class TestWiring(actorSystem: ActorSystem) extends MockitoSugar {

  lazy val testConfigs = new TestConfigs(actorSystem.settings.config)
  import testConfigs._

  lazy val wiring: Wiring = new Wiring(actorSystem)

  lazy val redisClient: RedisClient   = RedisClient.create()
  lazy val redisFactory: RedisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)
  lazy val kafkaFactory: KafkaFactory = new KafkaFactory(mock[LocationService], wiring)

  def publisher: EventPublisher =
    if (redisEnabled) redisFactory.publisher(redisHost, redisPort)
    else kafkaFactory.publisher(kafkaHost, kafkaPort)

  def subscriber: EventSubscriber =
    if (redisEnabled) redisFactory.subscriber(redisHost, redisPort)
    else kafkaFactory.subscriber(kafkaHost, kafkaPort)

}
