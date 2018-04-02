package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.KafkaFactory
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class PubSubTest extends FunSuite with EmbeddedKafka with BeforeAndAfterAll {
  private val seedPort        = 3561
  private val kafkaPort       = 6001
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, kafkaPort)
  locationService.register(tcpRegistration).await

  private implicit val actorSystem: ActorSystem = clusterSettings.system
  private val brokers                           = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  private val brokerProperties                  = Map("listeners" → brokers, "advertised.listeners" → brokers)

  private val config = EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)

  private val wiring       = new Wiring(actorSystem)
  private val kafkaFactory = new KafkaFactory(locationService, wiring)
  private val publisher    = kafkaFactory.publisher().await
  private val subscriber   = kafkaFactory.subscriber().await
  private val framework    = new EventServicePubSubTestFramework(publisher, subscriber)

  override def beforeAll(): Unit = {
    EmbeddedKafka.start()(config)
  }

  override def afterAll(): Unit = {
    publisher.shutdown().await
    EmbeddedKafka.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  // DEOPSCSW-334 : Publish an event
  test("should be able to publish and subscribe an event") {
    framework.pubSub()
  }

  test("Kafka independent subscriptions") {
    framework.subscribeIndependently()
  }

  test("should be able to publish concurrently to the same channel") {
    framework.publishMultiple()
  }

  test("should be able to publish concurrently to the different channel") {
    framework.publishMultipleToDifferentChannels()
  }

  test("Kafka retrieve recently published event on subscription") {
    framework.retrieveRecentlyPublished()
  }

  test("Kafka retrieveInvalidEvent") {
    framework.retrieveInvalidEvent()
  }

  // DEOPSCSW-334 : Publish an event
  test("should be able to get an event without subscribing for it") {
    framework.get()
  }

  test("Kakfa get retrieveInvalidEvent") {
    framework.retrieveInvalidEventOnget()
  }

}
