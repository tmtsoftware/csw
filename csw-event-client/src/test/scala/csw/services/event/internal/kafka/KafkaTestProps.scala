package csw.services.event.internal.kafka

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.kafka.ProducerSettings
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.wiring.BaseProperties
import csw.services.event.internal.wiring.BaseProperties.createInfra
import csw.services.event.javadsl.{IEventPublisher, IEventService, IEventSubscriber}
import csw.services.event.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.LocationService
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Future

class KafkaTestProps(
    kafkaPort: Int,
    clusterSettings: ClusterSettings,
    locationService: LocationService,
    additionalBrokerProps: Map[String, String]
)(implicit val actorSystem: ActorSystem)
    extends BaseProperties {
  private val brokers          = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
  private val brokerProperties = Map("listeners" → brokers, "advertised.listeners" → brokers) ++ additionalBrokerProps
  val config                   = EmbeddedKafkaConfig(customBrokerProperties = brokerProperties)

  private val eventServiceFactory = new KafkaEventServiceFactory()
  private lazy val producerSettings: ProducerSettings[String, String] =
    ProducerSettings(actorSystem, new StringSerializer, new StringSerializer)
      .withBootstrapServers(s"${clusterSettings.hostname}:$kafkaPort")

  private lazy val kafkaProducer = producerSettings.createKafkaProducer()

  val eventService: EventService   = eventServiceFactory.make(locationService)
  val jEventService: IEventService = eventServiceFactory.jMake(locationService.asJava, typedActorSystem)

  val publisher: EventPublisher                                    = eventService.defaultPublisher.await
  val subscriber: EventSubscriber                                  = eventService.defaultSubscriber.await
  override def jPublisher[T <: EventPublisher]: IEventPublisher    = jEventService.defaultPublisher.toScala.await
  override def jSubscriber[T <: EventSubscriber]: IEventSubscriber = jEventService.defaultSubscriber.toScala.await

  override def toString: String = "Kafka"

  override val eventPattern: String = ".*sys.*"

  override def publishGarbage(channel: String, message: String): Future[Done] =
    Future { kafkaProducer.send(new ProducerRecord(channel, message)).get() }.map(_ ⇒ Done)

  override def start(): Unit = EmbeddedKafka.start()(config)

  override def shutdown(): Unit = {
    EmbeddedKafka.stop()
    CoordinatedShutdown(actorSystem).run(TestFinishedReason).await
  }
}

object KafkaTestProps {

  def createKafkaProperties(
      seedPort: Int,
      serverPort: Int,
      additionalBrokerProps: Map[String, String] = Map.empty
  ): KafkaTestProps = {
    val (clusterSettings, locationService) = createInfra(seedPort, serverPort)
    new KafkaTestProps(serverPort, clusterSettings, locationService, additionalBrokerProps)(clusterSettings.system)
  }

  def jCreateKafkaProperties(
      seedPort: Int,
      serverPort: Int,
      additionalBrokerProps: java.util.Map[String, String]
  ): KafkaTestProps = createKafkaProperties(seedPort, serverPort, additionalBrokerProps.asScala.toMap)
}
