package csw.services.event.internal

import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.makeEvent
import csw.services.event.internal.commons.Wiring
import csw.services.event.internal.perf.Monitor
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import org.scalatest.Matchers

import scala.concurrent.duration.DurationLong

class EventServicePubSubTestFramework(wiring: Wiring) extends Matchers {

  import wiring._

  val eventKey: EventKey = makeEvent(0).eventKey
  var counter            = 0

  private val eventStream                 = Source.repeat(()).map(_ => eventGenerator())
  private val publisher: EventPublisher   = wiring.publisher()
  private val subscriber: EventSubscriber = wiring.subscriber()

  private def eventGenerator() = {
    counter += 1
    makeEvent(counter)
  }

  def monitorPerf(): Unit = {
    val doneF = subscriber
      .subscribe(Set(eventKey), 20.millis)
      // uncomment below line and EventSubscriber.subscribeWithSinkActorRef method to see the effects of subscribing with Sink.actorRef
      //.subscribeWithSinkActorRef(Set(eventKey), 20.millis)
      .via(Monitor.resetting)
      .runWith(Sink.ignore)

    Thread.sleep(1000)

    //    eventStream.mapAsync(1)(publisher.publish).runWith(Sink.ignore)
    //    publisher.publish(eventStream)
    publisher.publish(eventGenerator, 5.millis)

    doneF.await
  }

  def pubSub(): Unit = {
    val event1             = makeEvent(1)
    val eventKey: EventKey = event1.eventKey

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(2000)

    publisher.publish(event1).await
    Thread.sleep(1000)

    subscription.unsubscribe().await
    seqF.await shouldBe List(Event.invalidEvent, event1)
  }

  def subscribeIndependently(): Unit = {

    val prefix        = Prefix("test.prefix")
    val eventName1    = EventName("system1")
    val eventName2    = EventName("system2")
    val event1: Event = SystemEvent(prefix, eventName1)
    val event2: Event = SystemEvent(prefix, eventName2)

    val (subscription, seqF) = subscriber.subscribe(Set(event1.eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)
    publisher.publish(event1).await
    Thread.sleep(1000)

    val (subscription2, seqF2) = subscriber.subscribe(Set(event2.eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)
    publisher.publish(event2).await
    Thread.sleep(1000)

    subscription.unsubscribe().await
    subscription2.unsubscribe().await

    seqF.await shouldBe List(Event.invalidEvent, event1)
    seqF2.await shouldBe List(Event.invalidEvent, event2)
  }

  def publishMultiple(): Unit = {
    def event: Event = makeEvent(1)

    val eventKey: EventKey = event.eventKey

    subscriber.subscribe(Set(eventKey)).runForeach { x =>
      val begin = x.eventTime.time.toEpochMilli
      println(System.currentTimeMillis() - begin)
    }

    Thread.sleep(10)

    publisher.publish(Source.fromIterator(() => Iterator.continually(event)).map(x => { println(s"from 1 -> $x"); x }))
    publisher
      .publish(
        Source
          .fromIterator(() => Iterator.continually(event))
          .map(x => { println(s"from 2            -> $x"); x })
          .watchTermination()(Keep.right)
      )
      .await
  }

  def retrieveRecentlyPublished(): Unit = {
    val event1   = makeEvent(1)
    val event2   = makeEvent(2)
    val event3   = makeEvent(3)
    val eventKey = event1.eventKey

    publisher.publish(event1).await
    publisher.publish(event2).await

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    publisher.publish(event3).await
    Thread.sleep(1000)

    subscription.unsubscribe()

    seqF.await shouldBe Seq(event2, event3)
  }

  def retrieveInvalidEvent(): Unit = {
    val eventKey = EventKey("test")

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    subscription.unsubscribe()

    seqF.await shouldBe Seq(Event.invalidEvent)
  }

  def get(): Unit = {
    val event1   = makeEvent(1)
    val eventKey = event1.eventKey

    publisher.publish(event1).await

    val eventF = subscriber.get(eventKey)

    eventF.await shouldBe event1
  }

  def retrieveInvalidEventOnget(): Unit = {

    val eventF = subscriber.get(EventKey("test"))

    eventF.await shouldBe Event.invalidEvent
  }

}
