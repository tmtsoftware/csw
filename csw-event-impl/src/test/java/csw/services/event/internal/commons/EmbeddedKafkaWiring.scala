package csw.services.event.internal.commons

import csw.services.location.commons.ClusterSettings
import net.manub.embeddedkafka.EmbeddedKafkaConfig

object EmbeddedKafkaWiring {

  def embeddedKafkaConfig(clusterSettings: ClusterSettings): EmbeddedKafkaConfig = {
    val kafkaPort        = 6001
    val brokers          = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
    val brokerProperties = Map("listeners" → brokers, "advertised.listeners" → brokers)

    EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)
  }
}
