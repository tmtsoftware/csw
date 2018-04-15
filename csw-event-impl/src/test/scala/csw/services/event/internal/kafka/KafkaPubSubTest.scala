package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.event.scaladsl.KafkaFactory
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
class KafkaPubSubTest extends FunSuite with EmbeddedKafka with BeforeAndAfterAll {
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

  test("Kafka - should be able to publish and subscribe an event") {
    framework.pubSub()
  }

  test("Kafka - should be able to make independent subscriptions") {
    framework.subscribeIndependently()
  }

  test("Kafka - should be able to publish concurrently to the same channel") {
    framework.publishMultiple()
  }

  test("Kafka - should be able to publish concurrently to the different channel") {
    framework.publishMultipleToDifferentChannels()
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  test("Kafka - should be able to retrieve recently published event on subscription") {
    framework.retrieveRecentlyPublished()
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  test("Kafka - should be able to retrieve InvalidEvent") {
    framework.retrieveInvalidEvent()
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  test("Kafka - should be able to retrieve only valid events when one of the subscribed events keys has published events") {
    framework.retrieveMultipleSubscribedEvents()
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  test("Kafka - should be able to subscribe with callback") {
    framework.retrieveEventUsingCallback()
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  test("Kafka - should be able to subscribe with async callback") {
    framework.retrieveEventUsingAsyncCallback()
  }

  //DEOPSCSW-339: Provide actor ref to alert about Event arrival
  test("Kafka - should be able to subscribe with an ActorRef") {
    framework.retrieveEventUsingActorRef()
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  test("Kafka - should be able to get an event without subscribing for it") {
    framework.get()
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  test("Kafka - should be able to get InvalidEvent") {
    framework.retrieveInvalidEventOnGet()
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  test("Kafka - should be able to get events for multiple event keys") {
    framework.retrieveEventsForMultipleEventKeysOnGet()
  }
}
