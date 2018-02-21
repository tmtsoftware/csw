package csw.services.event.internal.kafka

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

  test("throughput") {
    val subscriber = kafkaSubscriber
    val publisher  = kafkaPublisher

    val prefix    = Prefix("test.prefix")
    val eventName = EventName("system")

    def event(x: Int): Event = SystemEvent(prefix, eventName).copy(eventId = Id(x.toString))

    val eventKey: EventKey = event(0).eventKey

    val (sub, seqF) = subscriber.subscribe(Set(eventKey)).map(x => { println(x); x }).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    val limit = 10

    val stream = Source(1 to limit).map(event)

    publisher.publish(stream).await
//    stream.mapAsync(1024)(publisher.publish).runForeach(_ => ()).await

//    val queue = publisher.queue()
//    stream.mapAsync(1)(queue.offer).runForeach(_ => ()).await
//    queue.complete()
//    queue.watchCompletion().await

    Thread.sleep(1000)
    sub.unsubscribe().await
    seqF.await.map(_.eventId.id.toInt) shouldBe (1 to limit).toList
  }

}
