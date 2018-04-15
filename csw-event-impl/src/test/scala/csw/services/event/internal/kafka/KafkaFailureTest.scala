package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.Source
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailed
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.{RegistrationFactory, Utils}
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.event.scaladsl.KafkaFactory
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
class KafkaFailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {
  private val seedPort        = 3559
  private val kafkaPort       = 6001
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, kafkaPort)
  locationService.register(tcpRegistration).await

  private implicit val actorSystem: ActorSystem = clusterSettings.system

  private val brokers          = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  private val brokerProperties = Map("listeners" → brokers, "advertised.listeners" → brokers, "message.max.bytes" → "1")

  private val config = EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)

  private val wiring       = new Wiring(actorSystem)
  private val kafkaFactory = new KafkaFactory(locationService, wiring)
  private val publisher    = kafkaFactory.publisher().await

  case class FailedEvent(event: Event, throwable: Throwable)

  override def beforeAll(): Unit = {
    EmbeddedKafka.start()(config)
  }

  override def afterAll(): Unit = {
    publisher.shutdown().await
    EmbeddedKafka.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  test("Kafka - failure in publishing should fail future with PublishFailed exception") {

    // simulate publishing failure as message size is greater than message.max.bytes(1 byte) configured in broker
    intercept[PublishFailed] {
      publisher.publish(Utils.makeEvent(2)).await
    }
  }

  test("Kafka - handle failed publish event with a callback") {

    val testProbe   = TestProbe[FailedEvent]()(actorSystem.toTyped)
    val event       = Utils.makeEvent(1)
    val eventStream = Source.single(event)

    publisher.publish(eventStream, (event, ex) ⇒ testProbe.ref ! FailedEvent(event, ex))

    val failedEvent = testProbe.expectMessageType[FailedEvent]

    failedEvent.event shouldBe event
    failedEvent.throwable shouldBe a[PublishFailed]
  }

  test("Kafka - handle failed publish event with an eventGenerator and a callback") {
    val testProbe = TestProbe[FailedEvent]()(actorSystem.toTyped)
    val event     = Utils.makeEvent(1)

    publisher.publish(event, 20.millis, (event, ex) ⇒ testProbe.ref ! FailedEvent(event, ex))

    val failedEvent = testProbe.expectMessageType[FailedEvent]

    failedEvent.event shouldBe event
    failedEvent.throwable shouldBe a[PublishFailed]
  }
}
