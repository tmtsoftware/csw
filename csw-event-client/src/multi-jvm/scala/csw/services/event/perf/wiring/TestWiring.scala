package csw.services.event.perf.wiring

import akka.actor
import akka.actor.typed.ActorSystem

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.kafka.KafkaEventServiceFactory
import csw.services.event.internal.redis.RedisEventServiceFactory
import csw.services.event.scaladsl._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

class TestWiring(val actorSystem: actor.ActorSystem) extends MockitoSugar {
  lazy val testConfigs = new TestConfigs(actorSystem.settings.config)
  import testConfigs._

  implicit lazy val typedActorSystem: ActorSystem[_] = actorSystem.toTyped
  implicit lazy val ec: ExecutionContext             = actorSystem.dispatcher
  lazy val settings: ActorMaterializerSettings =
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(Supervision.getResumingDecider)
  implicit lazy val resumingMat: Materializer = ActorMaterializer(settings)(actorSystem)

  private lazy val redisEventService = RedisEventServiceFactory.make(redisHost, redisPort)
  private lazy val kafkaEventService = KafkaEventServiceFactory.make(kafkaHost, kafkaPort)

  def publisher: EventPublisher =
    if (redisEnabled) redisEventService.makeNewPublisher().await else kafkaEventService.makeNewPublisher().await

  def subscriber: EventSubscriber =
    if (redisEnabled) redisEventService.defaultSubscriber.await else kafkaEventService.defaultSubscriber.await

}
