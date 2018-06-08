package csw.services.event.perf.wiring

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.internal.commons.EventServiceResolver
import csw.services.event.internal.kafka.KafkaEventService
import csw.services.event.internal.redis.RedisEventService
import csw.services.event.scaladsl._
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

class TestWiring(val actorSystem: ActorSystem) extends MockitoSugar {

  private val masterId = "eventServer"
  lazy val testConfigs = new TestConfigs(actorSystem.settings.config)
  import testConfigs._
  implicit lazy val ec: ExecutionContext = actorSystem.dispatcher
  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)(actorSystem)

  lazy val redisEventService    = new RedisEventService(RedisClient.create(), masterId, mock[EventServiceResolver])
  private val kafkaEventService = new KafkaEventService(mock[EventServiceResolver])(actorSystem, ec, resumingMat)

  def publisher: EventPublisher =
    if (redisEnabled) redisEventService.publisher(redisHost, redisPort)
    else kafkaEventService.publisher(kafkaHost, kafkaPort)

  def subscriber: EventSubscriber =
    if (redisEnabled) redisEventService.subscriber(redisHost, redisPort)
    else kafkaEventService.subscriber(kafkaHost, kafkaPort)

}
