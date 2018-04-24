package csw.services.event.internal.commons

import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory, RedisFactory}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import io.lettuce.core.RedisClient
import net.manub.embeddedkafka.EmbeddedKafkaConfig
import redis.embedded.RedisServer

trait BaseProperties {
  val wiring: Wiring
  def publisher: EventPublisher
  def subscriber: EventSubscriber
}

object BaseProperties {
  private def createInfra(seedPort: Int, serverPort: Int) = {
    val clusterSettings: ClusterSettings = ClusterAwareSettings.joinLocal(seedPort)
    val locationService                  = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
    val tcpRegistration                  = RegistrationFactory.tcp(EventServiceConnection.value, serverPort)
    locationService.register(tcpRegistration).await
    (clusterSettings, locationService)
  }

  def createRedisProperties(seedPort: Int, serverPort: Int): RedisTestProps = {
    val (clusterSettings: ClusterSettings, locationService: LocationService) = createInfra(seedPort, serverPort)
    new RedisTestProps(serverPort, clusterSettings, locationService)
  }

  def createKafkaProperties(seedPort: Int, serverPort: Int): KafkaTestProps = {
    val (clusterSettings: ClusterSettings, locationService: LocationService) = createInfra(seedPort, serverPort)
    new KafkaTestProps(serverPort, clusterSettings, locationService)
  }
}

class RedisTestProps(redisPort: Int, clusterSettings: ClusterSettings, locationService: LocationService) extends BaseProperties {
  val wiring                      = new Wiring(clusterSettings.system)
  val redis: RedisServer          = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()
  val redisClient: RedisClient    = RedisClient.create()
  val redisFactory                = new RedisFactory(redisClient, locationService, wiring)
  val publisher: EventPublisher   = redisFactory.publisher().await
  val subscriber: EventSubscriber = redisFactory.subscriber().await

  override def toString: String = "Redis"
}

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
