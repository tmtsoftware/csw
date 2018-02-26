package csw.services.event.internal.kafka

import akka.stream.scaladsl.{Keep, Sink}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils._
import csw.services.event.internal.Wiring
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class KafkaPubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedKafka {

  private val wiring   = new Wiring()
  private val eventKey = makeEvent(0).eventKey

  import wiring._
  EmbeddedKafka.start()

  override def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
    EmbeddedKafka.stop()
  }

  test("pub-sub") {
    val event1               = makeEvent(1)
    val (subscription, seqF) = kafkaSubscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(2000)

    kafkaPublisher.publish(event1).await

    subscription.unsubscribe().await
    seqF.await shouldBe List(event1)
  }

  test("Retrieve most recently published event on subscription") {
    val event1 = makeEvent(1)
    val event2 = makeEvent(2)
    val event3 = makeEvent(3)

    kafkaPublisher.publish(event1).await
    kafkaPublisher.publish(event2).await

    val (subscription, seqF) = kafkaSubscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(2000)

    kafkaPublisher.publish(event3).await
    Thread.sleep(1000)

    subscription.unsubscribe()

    seqF.await shouldBe Seq(event2, event3)
  }

}
