package csw.services.event.internal.kafka

import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.BaseProperties.createInfra
import csw.services.event.internal.wiring.{BaseProperties, EventServiceResolver, Wiring}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationService
import net.manub.embeddedkafka.EmbeddedKafkaConfig

import scala.collection.JavaConverters.mapAsScalaMapConverter

class KafkaTestProps(
    kafkaPort: Int,
    clusterSettings: ClusterSettings,
    locationService: LocationService,
    additionalBrokerProps: Map[String, String]
) extends BaseProperties {
  private val brokers          = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  private val brokerProperties = Map("listeners" → brokers, "advertised.listeners" → brokers) ++ additionalBrokerProps
  val config                   = EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)
  val wiring                   = new Wiring(clusterSettings.system)
  import wiring._
  private val eventPublisherUtil  = new EventPublisherUtil()
  private val eventSubscriberUtil = new EventSubscriberUtil()
  val kafkaFactory                = new KafkaFactory(new EventServiceResolver(locationService), eventPublisherUtil, eventSubscriberUtil)
  val publisher: EventPublisher   = kafkaFactory.publisher().await
  val subscriber: EventSubscriber = kafkaFactory.subscriber().await

  override def toString: String = "Kafka"
}

object KafkaTestProps {

  def createKafkaProperties(
      seedPort: Int,
      serverPort: Int,
      additionalBrokerProps: Map[String, String] = Map.empty
  ): KafkaTestProps = {
    val (clusterSettings, locationService) = createInfra(seedPort, serverPort)
    new KafkaTestProps(serverPort, clusterSettings, locationService, additionalBrokerProps)
  }

  def jCreateKafkaProperties(
      seedPort: Int,
      serverPort: Int,
      additionalBrokerProps: java.util.Map[String, String]
  ): KafkaTestProps = {
    val (clusterSettings, locationService) = createInfra(seedPort, serverPort)
    new KafkaTestProps(serverPort, clusterSettings, locationService, additionalBrokerProps.asScala.toMap)
  }
}
