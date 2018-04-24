package csw.services.event

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.{makeDistinctEvent, makeEvent}
import csw.services.event.internal.commons._
import csw.services.event.internal.kafka.KafkaTestProps
import csw.services.event.internal.redis.RedisTestProps
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatest.testng.TestNGSuite
import org.testng.annotations._

import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
class EventServiceTest extends TestNGSuite with Matchers with Eventually with EmbeddedKafka {

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  var redisTestProps: RedisTestProps = _
  var kafkaTestProps: KafkaTestProps = _

  @BeforeSuite
  def beforeAll(): Unit = {
    redisTestProps = RedisTestProps.createRedisProperties(3558, 6384)
    kafkaTestProps = KafkaTestProps.createKafkaProperties(3561, 6001)
    redisTestProps.redis.start()
    EmbeddedKafka.start()(kafkaTestProps.config)
  }

  @AfterSuite
  def afterAll(): Unit = {
    redisTestProps.redisClient.shutdown()
    redisTestProps.redis.stop()
    redisTestProps.wiring.shutdown(TestFinishedReason).await

    kafkaTestProps.publisher.shutdown().await
    EmbeddedKafka.stop()
    kafkaTestProps.wiring.shutdown(TestFinishedReason).await
  }

  @DataProvider(name = "event-service-provider")
  def pubSubProvider: Array[Array[_ <: BaseProperties]] = Array(
    Array(redisTestProps),
    Array(kafkaTestProps)
  )

  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_and_subscribe_an_event(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val event1             = makeDistinctEvent(1)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(actorSystem.toTyped)
    val subscription       = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()

    subscription.isReady.await
    publisher.publish(event1).await

    testProbe.expectMessageType[SystemEvent].isInvalid shouldBe true
    testProbe.expectMessage(event1)

    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(2.seconds)
  }

  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_make_independent_subscriptions(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val prefix        = Prefix("test.prefix")
    val eventName1    = EventName("system1")
    val eventName2    = EventName("system2")
    val event1: Event = SystemEvent(prefix, eventName1)
    val event2: Event = SystemEvent(prefix, eventName2)

    val (subscription, seqF) = subscriber.subscribe(Set(event1.eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription.isReady.await
    Thread.sleep(100)
    publisher.publish(event1).await

    val (subscription2, seqF2) = subscriber.subscribe(Set(event2.eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription2.isReady.await
    publisher.publish(event2).await

    seqF.await.toSet shouldBe Set(Event.invalidEvent(event1.eventKey), event1)
    seqF2.await.toSet shouldBe Set(Event.invalidEvent(event2.eventKey), event2)
  }

  var cancellable: Cancellable = _
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_concurrently_to_the_same_channel(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 1 to 10) yield makeEvent(i)

    def eventGenerator(): Event = {
      counter += 1
      if (counter == 10) cancellable.cancel()
      events(counter)
    }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = makeEvent(0).eventKey

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.isReady.await

    cancellable = publisher.publish(eventGenerator(), 2.millis)

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 11)

    queue should contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events
  }

  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_concurrently_to_the_different_channel(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val events: immutable.Seq[Event] = for (i ← 101 to 110) yield makeDistinctEvent(i)

    val subscription = subscriber.subscribe(events.map(_.eventKey).toSet).to(Sink.foreach(queue.enqueue(_))).run()
    subscription.isReady.await

    publisher.publish(Source.fromIterator(() ⇒ events.toIterator))

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 20)

    queue should contain theSameElementsAs events.map(x ⇒ Event.invalidEvent(x.eventKey)) ++ events
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_retrieve_recently_published_event_on_subscription(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val event1   = makeEvent(1)
    val event2   = makeEvent(2)
    val event3   = makeEvent(3)
    val eventKey = event1.eventKey

    publisher.publish(event1).await
    publisher.publish(event2).await // latest event before subscribing

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription.isReady.await

    publisher.publish(event3).await

    // assertion against a sequence ensures that the latest event before subscribing arrives earlier in the stream
    seqF.await shouldBe Seq(event2, event3)
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_retrieve_InvalidEvent(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._
    val eventKey = EventKey("test")

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).take(1).toMat(Sink.seq)(Keep.both).run()

    seqF.await shouldBe Seq(Event.invalidEvent(eventKey))
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_retrieve_valid_as_well_as_invalid_event_when_events_are_published_for_some_and_not_for_other_keys(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    import baseProperties.wiring._
    val distinctEvent1 = makeDistinctEvent(201)
    val distinctEvent2 = makeDistinctEvent(202)

    val eventKey1 = distinctEvent1.eventKey
    val eventKey2 = distinctEvent2.eventKey

    publisher.publish(distinctEvent1).await

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey1, eventKey2)).take(2).toMat(Sink.seq)(Keep.both).run()

    seqF.await.toSet shouldBe Set(Event.invalidEvent(eventKey2), distinctEvent1)
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_callback(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val event1 = makeDistinctEvent(203)

    val testProbe              = TestProbe[Event]()(actorSystem.toTyped)
    val callback: Event ⇒ Unit = testProbe.ref ! _

    publisher.publish(event1).await

    val subscription = subscriber.subscribeCallback(Set(event1.eventKey), callback)
    testProbe.expectMessage(event1)
    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(200.millis)
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_async_callback(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val event1    = makeDistinctEvent(204)
    val testProbe = TestProbe[Event]()(actorSystem.toTyped)

    val callback: Event ⇒ Future[Event] = (event) ⇒ Future.successful(testProbe.ref ! event).map(_ ⇒ event)(ec)

    publisher.publish(event1).await

    val subscription = subscriber.subscribeAsync(Set(event1.eventKey), callback)
    testProbe.expectMessage(event1)
    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(200.millis)
  }

  //DEOPSCSW-339: Provide actor ref to alert about Event arrival
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_an_ActorRef(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val event1 = makeDistinctEvent(205)
    import baseProperties.wiring._

    val probe = TestProbe[Event]()(actorSystem.toTyped)

    publisher.publish(event1).await

    val subscription = subscriber.subscribeActorRef(Set(event1.eventKey), probe.ref)
    probe.expectMessage(event1)
    subscription.unsubscribe().await

    publisher.publish(event1)
    probe.expectNoMessage(200.millis)
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_get_an_event_without_subscribing_for_it(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val event1   = makeDistinctEvent(301)
    val eventKey = event1.eventKey

    publisher.publish(event1).await

    val eventF = subscriber.get(eventKey)
    eventF.await shouldBe event1
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_get_InvalidEvent(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val prefix    = Prefix("wfos.blue.test_filter")
    val eventName = EventName("move")
    val eventF    = subscriber.get(EventKey(prefix, eventName))
    val event     = eventF.await.asInstanceOf[SystemEvent]

    event.isInvalid shouldBe true
    event.source shouldBe prefix
    event.eventName shouldBe eventName
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_get_events_for_multiple_event_keys(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val event1    = makeDistinctEvent(206)
    val eventKey1 = event1.eventKey

    val event2    = makeDistinctEvent(207)
    val eventKey2 = event2.eventKey

    publisher.publish(event1).await

    val eventsF = subscriber.get(Set(eventKey1, eventKey2))
    eventsF.await shouldBe Set(Event.invalidEvent(eventKey2), event1)
  }
}
