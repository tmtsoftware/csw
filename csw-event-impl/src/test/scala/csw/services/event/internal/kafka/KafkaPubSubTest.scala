package csw.services.event.internal.kafka

import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.{Id, Prefix}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.Wiring
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class KafkaPubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedKafka {

  private val wiring = new Wiring()

  import wiring._
  EmbeddedKafka.start()

  override def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
    EmbeddedKafka.stop()
  }

  test("pub-sub") {
    val prefix             = Prefix("test.prefix")
    val eventName          = EventName("system")
    val event              = SystemEvent(prefix, eventName)
    val eventKey: EventKey = event.eventKey

    val (subscription, seqF) = kafkaSubscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    kafkaPublisher.publish(event).await
    Thread.sleep(1000)

    subscription.unsubscribe().await
    seqF.await shouldBe List(event)
  }
}
