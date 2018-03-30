//package csw.services.event.internal.kafka
//
//import akka.actor.ActorSystem
//import csw.services.event.KafkaFactory
//import csw.services.event.exceptions.PublishFailed
//import csw.services.event.helpers.TestFutureExt.RichFuture
//import csw.services.event.helpers.{RegistrationFactory, Utils}
//import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
//import csw.services.location.commons.ClusterAwareSettings
//import csw.services.location.scaladsl.LocationServiceFactory
//import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
//import org.scalatest.mockito.MockitoSugar
//import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
//
//import scala.concurrent.duration.DurationInt
//
//class FailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {
//  private val seedPort        = 3559
//  private val kafkaPort       = 6001
//  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
//  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
//  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, kafkaPort)
//  locationService.register(tcpRegistration).await
//
//  private implicit val actorSystem: ActorSystem = clusterSettings.system
//
//  private val pubSubProperties = Map(
//    "bootstrap.servers"              → s"${clusterSettings.hostname}:$kafkaPort",
//    "kafka-clients.acks"             → "1",
//    "kafka-clients.linger.ms"        → "1",
//    "kafka-clients.compression.type" → "none",
//    "request.timeout.ms"             → "5",
//    "max.block.ms"                   → "5",
//    "batch.size"                     → "0"
//  )
//
//  private val consumerProperties = Map(
//    "bootstrap.servers" → s"${clusterSettings.hostname}:$kafkaPort"
//  )
//
//  private val brokers          = s"PLAINTEXT://${clusterSettings.hostname}:$kafkaPort"
//  private val brokerProperties = Map("listeners" → brokers, "advertised.listeners" → brokers, "request.timeout.ms" → "5")
//
//  private val config = EmbeddedKafkaConfig(customConsumerProperties = consumerProperties,
//                                           customProducerProperties = pubSubProperties,
//                                           customBrokerProperties = brokerProperties)
//
//  private val wiring       = new Wiring(actorSystem)
//  private val kafkaFactory = new KafkaFactory(locationService, wiring)
//  private val publisher    = kafkaFactory.publisher().await
//
//  override def beforeAll(): Unit = {
//    EmbeddedKafka.start()(config)
//  }
//
//  override def afterAll(): Unit = {
//    EmbeddedKafka.stop()
//  }
//
//  test("failure in kafka publishing") {
//
//    publisher.publish(Utils.makeEvent(1)).await
//
//    EmbeddedKafka.stop()
//
//    println(config.customProducerProperties)
//
//    val dd = intercept[PublishFailed] {
//      publisher.publish(Utils.makeEvent(2)).await(40.seconds)
//    }
//
//    println(dd)
//  }
//
//}
