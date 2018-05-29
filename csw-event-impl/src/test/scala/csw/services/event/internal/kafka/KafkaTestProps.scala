package csw.services.event.internal.kafka

import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.BaseProperties.createInfra
import csw.services.event.internal.wiring.{BaseProperties, EventServiceResolver, Wiring}
import csw.services.event.javadsl.{IEventPublisher, IEventSubscriber, JKafkaFactory}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber, KafkaFactory}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationService
import net.manub.embeddedkafka.EmbeddedKafkaConfig

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.compat.java8.FutureConverters.CompletionStageOps

class KafkaTestProps(
    kafkaPort: Int,
    kafkaFactory: KafkaFactory,
    jKafkaFactory: JKafkaFactory,
    clusterSettings: ClusterSettings,
    locationService: LocationService,
    additionalBrokerProps: Map[String, String]
) extends BaseProperties {
  private val brokers             = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  private val brokerProperties    = Map("listeners" → brokers, "advertised.listeners" → brokers) ++ additionalBrokerProps
  val config                      = EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)
  val wiring                      = new Wiring(clusterSettings.system)
  val publisher: EventPublisher   = kafkaFactory.publisher().await
  val subscriber: EventSubscriber = kafkaFactory.subscriber().await

  override def toString: String = "Kafka"

  override def jPublisher[T <: EventPublisher]: IEventPublisher = jKafkaFactory.publisher().toScala.await

  override def jSubscriber[T <: EventSubscriber]: IEventSubscriber = jKafkaFactory.subscriber().toScala.await
}

object KafkaTestProps {

  def createKafkaProperties(
      seedPort: Int,
      serverPort: Int,
      additionalBrokerProps: Map[String, String] = Map.empty
  ): KafkaTestProps = {
    val (clusterSettings, locationService) = createInfra(seedPort, serverPort)
    val wiring                             = new Wiring(clusterSettings.system)
    import wiring._
    val eventPublisherUtil  = new EventPublisherUtil()
    val eventSubscriberUtil = new EventSubscriberUtil()

    val kafkaFactory  = new KafkaFactory(new EventServiceResolver(locationService), eventPublisherUtil, eventSubscriberUtil)
    val jKafkaFactory = new JKafkaFactory(kafkaFactory)
    new KafkaTestProps(serverPort, kafkaFactory, jKafkaFactory, clusterSettings, locationService, additionalBrokerProps)
  }

  def jCreateKafkaProperties(
      seedPort: Int,
      serverPort: Int,
      additionalBrokerProps: java.util.Map[String, String]
  ): KafkaTestProps = createKafkaProperties(seedPort, serverPort, additionalBrokerProps.asScala.toMap)
}
