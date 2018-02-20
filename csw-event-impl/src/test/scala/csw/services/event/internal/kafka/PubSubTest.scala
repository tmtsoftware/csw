package csw.services.event.internal.kafka

import csw.messages.ccs.events.{EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.Wiring
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class PubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedKafka {

  private val wiring = new Wiring()

  import wiring._
//  implicit val userDefinedConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 9092, zooKeeperPort = 2181)
//  EmbeddedKafka.start()

  override def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
    EmbeddedKafka.stop()
  }

  test("pub-sub") {
    val prefix             = Prefix("test.prefix")
    val eventName          = EventName("system")
    val event              = SystemEvent(prefix, eventName)
    val eventKey: EventKey = event.eventKey

//    kafkaPublisher.publish(event).await
    println()
//    val (subscription, seqF) = kafkaSubscriberDriver.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
//    Thread.sleep(1000)
//
//    subscription.unsubscribe().await
//    seqF.await shouldBe Set(event)
  }

}
