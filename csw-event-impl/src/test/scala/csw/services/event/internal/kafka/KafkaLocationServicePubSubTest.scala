package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import csw.services.event.KafkaFactory
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import csw.services.event.internal.commons.EventServiceConnection
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class KafkaLocationServicePubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedKafka {
  private val seedPort  = 3559
  private val kafkaPort = 6001

  private val clusterSettings                   = ClusterAwareSettings.joinLocal(seedPort)
  private implicit val actorSystem: ActorSystem = clusterSettings.system
  EmbeddedKafka.start()

  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val kafkaFactory    = new KafkaFactory(locationService, actorSystem)
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, kafkaPort)
  locationService.register(tcpRegistration).await
  private var publisher  = kafkaFactory.publisher().await
  private var subscriber = kafkaFactory.subscriber().await
  private var framework  = new EventServicePubSubTestFramework(publisher, subscriber)

  override def afterAll(): Unit = {
    publisher.shutdown().await
    EmbeddedKafka.stop()
    actorSystem.terminate().await
  }

  ignore("Pub Sub") {
    framework.pubSub()
  }

}
