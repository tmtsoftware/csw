package csw.services.event.internal.wiring

import csw.services.location.commons.ClusterSettings
import net.manub.embeddedkafka.EmbeddedKafkaConfig

object EmbeddedKafkaWiring {

  val kafkaPort = 6001

  def embeddedKafkaConfig(clusterSettings: ClusterSettings): EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(customBrokerProperties = defaultBrokerProperties(clusterSettings.hostname))

  def embeddedKafkaConfigForFailure(clusterSettings: ClusterSettings): EmbeddedKafkaConfig = EmbeddedKafkaConfig(
    customBrokerProperties = defaultBrokerProperties(clusterSettings.hostname) + ("message.max.bytes" → "1")
  )

  private def defaultBrokerProperties(hostName: String) = {
    val brokers = s"PLAINTEXT://$hostName:$kafkaPort"
    Map("listeners" → brokers, "advertised.listeners" → brokers)
  }
}
