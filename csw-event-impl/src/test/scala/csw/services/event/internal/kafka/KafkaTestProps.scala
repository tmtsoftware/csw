package csw.services.event.internal.kafka

import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.commons.BaseProperties.createInfra
import csw.services.event.internal.commons.{BaseProperties, Wiring}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationService
import net.manub.embeddedkafka.EmbeddedKafkaConfig

class KafkaTestProps(kafkaPort: Int, clusterSettings: ClusterSettings, locationService: LocationService) extends BaseProperties {
  val brokers                     = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  val brokerProperties            = Map("listeners" → brokers, "advertised.listeners" → brokers)
  val config                      = EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)
  val wiring                      = new Wiring(clusterSettings.system)
  val kafkaFactory                = new KafkaFactory(locationService, wiring)
  val publisher: EventPublisher   = kafkaFactory.publisher().await
  val subscriber: EventSubscriber = kafkaFactory.subscriber().await

  override def toString: String = "Kafka"
}

object KafkaTestProps {
  def createKafkaProperties(seedPort: Int, serverPort: Int): KafkaTestProps = {
    val (clusterSettings: ClusterSettings, locationService: LocationService) = createInfra(seedPort, serverPort)
    new KafkaTestProps(serverPort, clusterSettings, locationService)
  }
}
