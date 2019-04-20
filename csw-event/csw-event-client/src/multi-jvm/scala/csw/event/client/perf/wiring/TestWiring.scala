package csw.event.client.perf.wiring

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.typed.scaladsl
import akka.stream.{ActorMaterializerSettings, Materializer, Supervision}
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber}
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.{KafkaStore, RedisStore}
import org.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

class TestWiring(val actorSystem: ActorSystem[_]) extends MockitoSugar {
  lazy val testConfigs: TestConfigs = new TestConfigs(actorSystem.settings.config)
  import testConfigs._

  implicit lazy val ec: ExecutionContext = actorSystem.executionContext
  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem.toUntyped).withSupervisionStrategy(Supervision.getResumingDecider)
  implicit lazy val resumingMat: Materializer = scaladsl.ActorMaterializer(Some(settings))(actorSystem)

  private lazy val redisEventService = new EventServiceFactory(RedisStore()).make(redisHost, redisPort)(actorSystem)
  private lazy val kafkaEventService = new EventServiceFactory(KafkaStore).make(kafkaHost, kafkaPort)(actorSystem)

  def publisher: EventPublisher =
    if (redisEnabled) redisEventService.makeNewPublisher() else kafkaEventService.makeNewPublisher()

  def subscriber: EventSubscriber =
    if (redisEnabled) redisEventService.defaultSubscriber else kafkaEventService.defaultSubscriber

}
