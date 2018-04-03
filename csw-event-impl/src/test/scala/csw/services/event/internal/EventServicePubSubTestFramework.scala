package csw.services.event.internal

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.{makeDistinctEvent, makeEvent}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import org.scalatest.Matchers

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.DurationLong

class EventServicePubSubTestFramework(publisher: EventPublisher, subscriber: EventSubscriber)(
    implicit val actorSystem: ActorSystem
) extends Matchers {

  private implicit val mat: ActorMaterializer = ActorMaterializer()

  // DEOPSCSW-334 : Publish an event
  def pubSub(): Unit = {
    val event1             = makeDistinctEvent(1)
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

  var cancellable: Cancellable = _
  def publishMultiple(): Unit = {
    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 1 to 10) yield makeEvent(i)

    def eventGenerator(): Event = {
      counter += 1
      if (counter == 10) cancellable.cancel()
      events(counter)
    }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = makeEvent(0).eventKey

    subscriber.subscribe(Set(eventKey)).runForeach { x =>
      queue.enqueue(x)
    }

    Thread.sleep(10)

    cancellable = publisher.publish(eventGenerator, 2.millis)

    Thread.sleep(1000) //TODO : Try to replace with Await

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    queue.size shouldBe 11

    queue should contain allElementsOf Seq(Event.invalidEvent) ++ events
  }

  def publishMultipleToDifferentChannels(): Unit = {
    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val events: immutable.Seq[Event] = for (i ← 101 to 110) yield makeDistinctEvent(i)

    subscriber.subscribe(events.map(_.eventKey).toSet).runForeach { x =>
      queue.enqueue(x)
    }

    Thread.sleep(500)

    publisher.publish(Source.fromIterator(() ⇒ events.toIterator))

    Thread.sleep(1000)

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    queue.size shouldBe 11

    queue should contain theSameElementsAs Seq(Event.invalidEvent) ++ events
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

    subscription.unsubscribe().await

    seqF.await shouldBe Seq(event2, event3)
  }

  def retrieveInvalidEvent(): Unit = {
    val eventKey = EventKey("test")

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    subscription.unsubscribe().await

    seqF.await shouldBe Seq(Event.invalidEvent)
  }

  def get(): Unit = {
    val event1   = makeEvent(1)
    val eventKey = event1.eventKey

    publisher.publish(event1).await

    val eventF = subscriber.get(eventKey)

    eventF.await shouldBe event1
  }

  def retrieveInvalidEventOnGet(): Unit = {

    val eventF = subscriber.get(EventKey("test"))

    eventF.await shouldBe Event.invalidEvent
  }

}
