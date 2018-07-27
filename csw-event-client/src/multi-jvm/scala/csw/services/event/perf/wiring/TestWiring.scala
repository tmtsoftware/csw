package csw.services.event.perf.wiring

import akka.actor
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.EventServiceFactory
import csw.services.event.api.scaladsl.{EventPublisher, EventSubscriber}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.models.EventStores.{KafkaStore, RedisStore}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

class TestWiring(val actorSystem: actor.ActorSystem) extends MockitoSugar {
  lazy val testConfigs = new TestConfigs(actorSystem.settings.config)
  import testConfigs._

  implicit lazy val ec: ExecutionContext = actorSystem.dispatcher
  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)(actorSystem)

  private lazy val redisEventService = new EventServiceFactory(RedisStore()).make(redisHost, redisPort)(actorSystem)
  private lazy val kafkaEventService = new EventServiceFactory(KafkaStore).make(kafkaHost, kafkaPort)(actorSystem)

  def publisher: EventPublisher =
    if (redisEnabled) redisEventService.makeNewPublisher().await else kafkaEventService.makeNewPublisher().await

  def subscriber: EventSubscriber =
    if (redisEnabled) redisEventService.defaultSubscriber.await else kafkaEventService.defaultSubscriber.await

}
