package csw.services.event.perf.wiring

import akka.actor.ActorSystem
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.{EventServiceResolver, Wiring}
import csw.services.event.scaladsl._
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

class TestWiring(actorSystem: ActorSystem) extends MockitoSugar {

  lazy val testConfigs = new TestConfigs(actorSystem.settings.config)
  import testConfigs._
  val wiring = new Wiring(actorSystem)
  import wiring._

  private val masterId            = "mymaster"
  private val eventPublisherUtil  = new EventPublisherUtil()
  private val eventSubscriberUtil = new EventSubscriberUtil()

  lazy val redisFactory: RedisSentinelFactory =
    new RedisSentinelFactory(RedisClient.create(), mock[EventServiceResolver], eventPublisherUtil, eventSubscriberUtil)

  lazy val kafkaFactory: KafkaFactory =
    new KafkaFactory(mock[EventServiceResolver], eventPublisherUtil, eventSubscriberUtil)(actorSystem, ec, resumingMat)

  def publisher: EventPublisher =
    if (redisEnabled) redisFactory.publisher(redisHost, redisPort, masterId)
    else kafkaFactory.publisher(kafkaHost, kafkaPort)

  def subscriber: EventSubscriber =
    if (redisEnabled) redisFactory.subscriber(redisHost, redisPort, masterId)
    else kafkaFactory.subscriber(kafkaHost, kafkaPort)

}
