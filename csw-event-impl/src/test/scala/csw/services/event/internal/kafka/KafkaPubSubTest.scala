package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import csw.messages.models.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.KafkaFactory
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class KafkaPubSubTest extends FunSuite with EmbeddedKafka with BeforeAndAfterAll {
  private val seedPort        = 3559
  private val kafkaPort       = 6001
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, kafkaPort)
  locationService.register(tcpRegistration).await

  private implicit val actorSystem: ActorSystem = clusterSettings.system
  private val pubSubProperties                  = Map("bootstrap.servers" → s"${clusterSettings.hostname}:$kafkaPort")
  private val brokers                           = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  private val brokerProperties                  = Map("listeners" → brokers, "advertised.listeners" → brokers)

  private val config = EmbeddedKafkaConfig(customConsumerProperties = pubSubProperties,
                                           customProducerProperties = pubSubProperties,
                                           customBrokerProperties = brokerProperties)

  EmbeddedKafka.start()(config)

  private val wiring       = new Wiring(actorSystem)
  private val kafkaFactory = new KafkaFactory(locationService, wiring)
  private val publisher    = kafkaFactory.publisher().await
  private val subscriber   = kafkaFactory.subscriber().await
  private val framework    = new EventServicePubSubTestFramework(publisher, subscriber)

  override def afterAll(): Unit = {
    publisher.shutdown().await
    EmbeddedKafka.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  test("Kafka pub sub") {
    framework.pubSub()
  }

  test("Kafka independent subscriptions") {
    framework.subscribeIndependently()
  }

  ignore("Kafka multiple publish") {
    framework.publishMultiple()
  }

  test("Kafka retrieve recently published event on subscription") {
    framework.retrieveRecentlyPublished()
  }

  test("Kafka retrieveInvalidEvent") {
    framework.retrieveInvalidEvent()
  }

  test("Kakfa get") {
    framework.get()
  }

  test("Kakfa get retrieveInvalidEvent") {
    framework.retrieveInvalidEventOnget()
  }

}
