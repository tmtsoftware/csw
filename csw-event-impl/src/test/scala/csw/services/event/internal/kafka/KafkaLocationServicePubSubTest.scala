package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import csw.services.event.KafkaFactory
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import csw.services.event.internal.commons.EventServiceConnection
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class KafkaLocationServicePubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedKafka {
  private val seedPort         = 3559
  private val kafkaPort        = 6001
  private val clusterSettings  = ClusterAwareSettings.joinLocal(seedPort)
  private val pubSubProperties = Map("bootstrap.servers" → s"${clusterSettings.hostname}:$kafkaPort")
  private val brokers          = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  private val brokerProperties = Map("listeners" → brokers, "advertised.listeners" → brokers)

  private implicit val actorSystem: ActorSystem = clusterSettings.system
  private val locationService: LocationService  = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))

  private val config = EmbeddedKafkaConfig(customConsumerProperties = pubSubProperties,
                                           customProducerProperties = pubSubProperties,
                                           customBrokerProperties = brokerProperties)

  EmbeddedKafka.start()(config)

  private val kafkaFactory    = new KafkaFactory(locationService, actorSystem)
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, kafkaPort)
  locationService.register(tcpRegistration).await
  private val publisher  = kafkaFactory.publisher().await
  private val subscriber = kafkaFactory.subscriber().await
  private val framework  = new EventServicePubSubTestFramework(publisher, subscriber)

  override def afterAll(): Unit = {
    publisher.shutdown().await
    EmbeddedKafka.stop()
    actorSystem.terminate().await
  }

  test("Pub Sub") {
    framework.pubSub()
  }
}
