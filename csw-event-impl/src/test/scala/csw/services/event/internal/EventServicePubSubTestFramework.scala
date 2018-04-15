package csw.services.event.internal

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.{makeDistinctEvent, makeEvent}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually

import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class EventServicePubSubTestFramework(publisher: EventPublisher, subscriber: EventSubscriber)(
    implicit val actorSystem: ActorSystem
) extends Matchers
    with Eventually {

  private implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val patience: PatienceConfig       = PatienceConfig(5.seconds, 10.millis)

  def pubSub(): Unit = {
    val event1             = makeDistinctEvent(1)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(actorSystem.toTyped)
    val subscription       = subscriber.subscribe(Set(eventKey)).take(2).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()

    Thread.sleep(1000)
    publisher.publish(event1).await

    testProbe.expectMessageType[SystemEvent].isInvalid shouldBe true
    testProbe.expectMessage(event1)

    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(2.seconds)
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

    val (subscription2, seqF2) = subscriber.subscribe(Set(event2.eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)
    publisher.publish(event2).await

    subscription.unsubscribe().await
    subscription2.unsubscribe().await

    seqF.await.toSet shouldBe Set(Event.invalidEvent(event1.eventKey), event1)
    seqF2.await.toSet shouldBe Set(Event.invalidEvent(event2.eventKey), event2)
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

    Thread.sleep(100)

    cancellable = publisher.publish(eventGenerator(), 2.millis)
    Thread.sleep(500)

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 11)

    queue should contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events
  }

  def publishMultipleToDifferentChannels(): Unit = {
    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val events: immutable.Seq[Event] = for (i ← 101 to 110) yield makeDistinctEvent(i)

    subscriber.subscribe(events.map(_.eventKey).toSet).runForeach { x =>
      queue.enqueue(x)
    }
    Thread.sleep(500)

    publisher.publish(Source.fromIterator(() ⇒ events.toIterator))

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 20)

    queue should contain theSameElementsAs events.map(x ⇒ Event.invalidEvent(x.eventKey)) ++ events
  }

  def retrieveRecentlyPublished(): Unit = {
    val event1   = makeEvent(1)
    val event2   = makeEvent(2)
    val event3   = makeEvent(3)
    val eventKey = event1.eventKey

    publisher.publish(event1).await
    publisher.publish(event2).await // latest event before subscribing

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    publisher.publish(event3).await
    Thread.sleep(500)

    subscription.unsubscribe().await

    // assertion against a sequence ensures that the latest event before subscribing arrives earlier in the stream
    seqF.await shouldBe Seq(event2, event3)
  }

  def retrieveInvalidEvent(): Unit = {
    val eventKey = EventKey("test")

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    subscription.unsubscribe().await

    seqF.await shouldBe Seq(Event.invalidEvent(eventKey))
  }

  def retrieveMultipleSubscribedEvents(): Unit = {
    val distinctEvent1 = makeDistinctEvent(201)
    val distinctEvent2 = makeDistinctEvent(202)

    val eventKey1 = distinctEvent1.eventKey
    val eventKey2 = distinctEvent2.eventKey

    publisher.publish(distinctEvent1).await

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey1, eventKey2)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    subscription.unsubscribe().await

    seqF.await.toSet shouldBe Set(Event.invalidEvent(eventKey2), distinctEvent1)
  }

  def retrieveEventUsingCallback(): Unit = {
    val event1 = makeDistinctEvent(203)

    val testProbe              = TestProbe[Event]()(actorSystem.toTyped)
    val callback: Event ⇒ Unit = testProbe.ref ! _

    publisher.publish(event1).await

    val subscription = subscriber.subscribeCallback(Set(event1.eventKey), callback)
    Thread.sleep(1000)

    testProbe.expectMessage(event1)
    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(2.seconds)
  }

  def retrieveEventUsingAsyncCallback(): Unit = {
    val event1    = makeDistinctEvent(204)
    val testProbe = TestProbe[Event]()(actorSystem.toTyped)
    import actorSystem.dispatcher

    val callback: Event ⇒ Future[Event] = (event) ⇒ Future.successful(testProbe.ref ! event).map(_ ⇒ event)

    publisher.publish(event1).await

    val subscription = subscriber.subscribeAsync(Set(event1.eventKey), callback)
    Thread.sleep(1000)

    testProbe.expectMessage(event1)

    subscription.unsubscribe().await
    publisher.publish(event1).await

    testProbe.expectNoMessage(2.seconds)
  }

  def retrieveEventUsingActorRef(): Unit = {
    val event1 = makeDistinctEvent(205)
    val probe  = TestProbe[Event]()(actorSystem.toTyped)

    publisher.publish(event1).await

    val subscription = subscriber.subscribeActorRef(Set(event1.eventKey), probe.ref)
    Thread.sleep(1000)

    probe.expectMessage(event1)

    subscription.unsubscribe().await

    publisher.publish(event1)

    probe.expectNoMessage(2.seconds)
  }

  def get(): Unit = {
    val event1   = makeEvent(1)
    val eventKey = event1.eventKey

    publisher.publish(event1).await

    val eventF = subscriber.get(eventKey)

    eventF.await shouldBe event1
  }

  def retrieveInvalidEventOnGet(): Unit = {
    val prefix    = Prefix("wfos.blue.test_filter")
    val eventName = EventName("move")
    val eventF    = subscriber.get(EventKey(prefix, eventName))
    val event     = eventF.await.asInstanceOf[SystemEvent]

    event.isInvalid shouldBe true
    event.source shouldBe prefix
    event.eventName shouldBe eventName
  }

  def retrieveEventsForMultipleEventKeysOnGet(): Unit = {
    val event1    = makeDistinctEvent(206)
    val eventKey1 = event1.eventKey

    val event2    = makeDistinctEvent(207)
    val eventKey2 = event2.eventKey

    publisher.publish(event1).await

    val eventsF = subscriber.get(Set(eventKey1, eventKey2))

    eventsF.await shouldBe Set(Event.invalidEvent(eventKey2), event1)
  }
}
