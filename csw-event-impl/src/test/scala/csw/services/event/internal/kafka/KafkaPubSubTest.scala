package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class KafkaPubSubTest extends FunSuite with EmbeddedKafka with BeforeAndAfterAll {
  private lazy val actorSystem = ActorSystem()

  private val kafkaPort = 6001
  private val wiring    = new KafkaWiring("localhost", kafkaPort, actorSystem)
  private val publisher = wiring.publisher()
  private val framework = new EventServicePubSubTestFramework(wiring)

  override protected def beforeAll(): Unit = {
    EmbeddedKafka.start()
  }

  override def afterAll(): Unit = {
    publisher.shutdown().await
    EmbeddedKafka.stop()
    actorSystem.terminate()
  }

  ignore("throughput latency") {
    framework.monitorPerf()
  }

  test("Kafka pub sub") {
    framework.pubSub()
  }

  test("Kafka independent subscriptions") {
    framework.subscribeIndependently()
  }

  ignore("Kafka multiple publish") {
    framework.publishMultiple()
  }

  test("Kafka retrieve recently published event on subscription") {
    framework.retrieveRecentlyPublished()
  }

  test("Kafka retrieveInvalidEvent") {
    framework.retrieveInvalidEvent()
  }

  test("Kakfa get") {
    framework.get()
  }

  test("Kakfa get retrieveInvalidEvent") {
    framework.retrieveInvalidEventOnget()
  }

}
