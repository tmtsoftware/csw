package csw.services.event.internal.commons

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory, RedisFactory}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import io.lettuce.core.RedisClient
import net.manub.embeddedkafka.EmbeddedKafkaConfig
import redis.embedded.RedisServer

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait BaseProperties {
  def publisher: EventPublisher
  def subscriber: EventSubscriber
  def actorSystem: ActorSystem
  def mat: Materializer
  def ec: ExecutionContext
}

object RedisTestProperties extends BaseProperties {
  private val seedPort        = 3558
  private val redisPort       = 6384
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, redisPort)
  locationService.register(tcpRegistration).await

  val redis: RedisServer = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()

  implicit val actorSystem: ActorSystem     = clusterSettings.system
  implicit val mat: ActorMaterializer       = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher

  val redisClient: RedisClient = RedisClient.create()
  val wiring                   = new Wiring(actorSystem)
  val redisFactory             = new RedisFactory(redisClient, locationService, wiring)

  val publisher: EventPublisher   = redisFactory.publisher().await
  val subscriber: EventSubscriber = redisFactory.subscriber().await

  override def toString: String = "Redis"
}

object KafkaTestProperties extends BaseProperties {
  private val seedPort        = 3561
  private val kafkaPort       = 6001
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, kafkaPort)
  locationService.register(tcpRegistration).await

  implicit val actorSystem: ActorSystem     = clusterSettings.system
  implicit val mat: ActorMaterializer       = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher

  val brokers          = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  val brokerProperties = Map("listeners" → brokers, "advertised.listeners" → brokers)

  val config = EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)

  val wiring                      = new Wiring(actorSystem)
  val kafkaFactory                = new KafkaFactory(locationService, wiring)
  val publisher: EventPublisher   = kafkaFactory.publisher().await
  val subscriber: EventSubscriber = kafkaFactory.subscriber().await

  override def toString: String = "Kafka"

}
