package csw.services.event

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.{Keep, Sink}
import akka.testkit.typed.scaladsl.{TestInbox, TestProbe}
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.{makeDistinctEvent, makeEvent}
import csw.services.event.internal.kafka.KafkaTestProps
import csw.services.event.internal.redis.RedisTestProps
import csw.services.event.internal.wiring._
import csw.services.event.scaladsl.SubscriptionMode
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatest.testng.TestNGSuite
import org.testng.annotations._

import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Random

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
class EventSubscriberTest extends TestNGSuite with Matchers with Eventually with EmbeddedKafka {

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  var redisTestProps: RedisTestProps = _
  var kafkaTestProps: KafkaTestProps = _

  @BeforeSuite
  def beforeAll(): Unit = {
    redisTestProps = RedisTestProps.createRedisProperties(3564, 6383)
    kafkaTestProps = KafkaTestProps.createKafkaProperties(3565, 6003)
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

  val events: immutable.Seq[Event] = for (i ← 1 to 1500) yield makeEvent(i)

  def eventGenerator(beginAt: Int): Event = {
    var counter = beginAt
    counter += 1
    events(counter)
  }

  //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
  //DEOPSCSW-343: Unsubscribe based on prefix and event name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_and_subscribe_an_event(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val event1             = makeDistinctEvent(1)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(actorSystem.toTyped)
    val subscription       = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()

    subscription.ready.await
    publisher.publish(event1).await

    testProbe.expectMessageType[SystemEvent].isInvalid shouldBe true
    testProbe.expectMessage(event1)

    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(2.seconds)
  }

  //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_and_subscribe_an_event_with_duration(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val queue2: mutable.Queue[Event] = new mutable.Queue[Event]()
    val event1                       = makeDistinctEvent(Random.nextInt())
    val eventKey: EventKey           = event1.eventKey

    val cancellable = publisher.publish(eventGenerator(0), 1.millis)
    val subscription = subscriber
      .subscribe(Set(eventKey), 300.millis, SubscriptionMode.RateAdapterMode)
      .to(Sink.foreach[Event](queue.enqueue(_)))
      .run()

    subscription.ready().await

    val subscription2 = subscriber
      .subscribe(Set(eventKey), 400.millis, SubscriptionMode.RateAdapterMode)
      .to(Sink.foreach[Event](queue2.enqueue(_)))
      .run()
    subscription2.ready().await

    Thread.sleep(1000)

    subscription.unsubscribe().await
    subscription2.unsubscribe().await
    cancellable.cancel()

    queue.size shouldBe 3
    queue2.size shouldBe 2
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  //DEOPSCSW-343: Unsubscribe based on prefix and event name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_async_callback(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val event1    = makeDistinctEvent(204)
    val testProbe = TestProbe[Event]()(actorSystem.toTyped)

    val callback: Event ⇒ Future[Event] = (event) ⇒ Future.successful(testProbe.ref ! event).map(_ ⇒ event)(ec)

    publisher.publish(event1).await

    Thread.sleep(500) // Needed for redis set which is fire and forget operation

    val subscription = subscriber.subscribeAsync(Set(event1.eventKey), callback)
    testProbe.expectMessage(event1)
    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(200.millis)
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_async_callback_with_duration(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val queue2: mutable.Queue[Event] = new mutable.Queue[Event]()
    val event1                       = makeEvent(1)

    val callback: Event ⇒ Future[Event]  = (event) ⇒ Future.successful(queue.enqueue(event)).map(_ ⇒ event)(ec)
    val callback2: Event ⇒ Future[Event] = (event) ⇒ Future.successful(queue2.enqueue(event)).map(_ ⇒ event)(ec)

    val cancellable = publisher.publish(eventGenerator(0), 1.millis)

    val subscription  = subscriber.subscribeAsync(Set(event1.eventKey), callback, 300.millis, SubscriptionMode.RateAdapterMode)
    val subscription2 = subscriber.subscribeAsync(Set(event1.eventKey), callback2, 400.millis, SubscriptionMode.RateAdapterMode)
    Thread.sleep(1000) // Future in callback needs time to execute
    subscription.unsubscribe().await
    subscription2.unsubscribe().await

    cancellable.cancel()
    queue.size shouldBe 3
    queue2.size shouldBe 2
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
    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val subscription = subscriber.subscribeCallback(Set(event1.eventKey), callback)
    testProbe.expectMessage(event1)
    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(200.millis)
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_callback_with_duration(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val queue2: mutable.Queue[Event] = new mutable.Queue[Event]()
    val event1                       = makeEvent(1)

    val callback: Event ⇒ Unit  = queue.enqueue(_)
    val callback2: Event ⇒ Unit = queue2.enqueue(_)

    val cancellable = publisher.publish(eventGenerator(0), 1.millis)
    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val subscription = subscriber.subscribeCallback(Set(event1.eventKey), callback, 300.millis, SubscriptionMode.RateAdapterMode)
    val subscription2 =
      subscriber.subscribeCallback(Set(event1.eventKey), callback2, 400.millis, SubscriptionMode.RateAdapterMode)
    Thread.sleep(1000)
    subscription.unsubscribe().await
    subscription2.unsubscribe().await

    cancellable.cancel()
    queue.size shouldBe 3
    queue2.size shouldBe 2
  }

  //DEOPSCSW-339: Provide actor ref to alert about Event arrival
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_an_ActorRef(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val event1 = makeDistinctEvent(205)
    import baseProperties.wiring._

    val probe = TestProbe[Event]()(actorSystem.toTyped)

    publisher.publish(event1).await
    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val subscription = subscriber.subscribeActorRef(Set(event1.eventKey), probe.ref)
    probe.expectMessage(event1)
    subscription.unsubscribe().await

    publisher.publish(event1)
    probe.expectNoMessage(200.millis)
  }

  //DEOPSCSW-339: Provide actor ref to alert about Event arrival
  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_an_ActorRef_with_duration(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    val inbox  = TestInbox[Event]()
    val event1 = makeEvent(205)

    publisher.publish(event1).await
    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val subscription = subscriber.subscribeActorRef(Set(event1.eventKey), inbox.ref, 300.millis, SubscriptionMode.RateAdapterMode)
    Thread.sleep(1000)
    subscription.unsubscribe().await

    inbox.receiveAll().size shouldBe 3
  }

  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_rate_limiter_mode(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    val inbox = TestInbox[Event]()

    val cancellable = publisher.publish(eventGenerator(1), 200.millis)
    val subscription =
      subscriber.subscribeActorRef(events.map(_.eventKey).toSet, inbox.ref, 100.millis, SubscriptionMode.RateLimiterMode)
    Thread.sleep(900)
    subscription.unsubscribe().await
    cancellable.cancel()
    inbox.receiveAll().size shouldBe 5
  }

  def should_be_able_to_subscribe_with_pattern(): Unit = {
    val redisProps = redisTestProps
    import redisProps._
    import redisProps.wiring._

    val event1             = makeEvent(1)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(actorSystem.toTyped)
    val subscription =
      subscriber.pSubscribe("*test*").toMat(Sink.foreach(e ⇒ { println(e); testProbe.ref ! e }))(Keep.left).run()

    subscription.ready.await
    publisher.publish(event1).await

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
    subscription.ready.await
    Thread.sleep(100)
    publisher.publish(event1).await

    val (subscription2, seqF2) = subscriber.subscribe(Set(event2.eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription2.ready.await
    publisher.publish(event2).await

    seqF.await.toSet shouldBe Set(Event.invalidEvent(event1.eventKey), event1)
    seqF2.await.toSet shouldBe Set(Event.invalidEvent(event2.eventKey), event2)
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

    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription.ready.await

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
    Thread.sleep(500)

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey1, eventKey2)).take(2).toMat(Sink.seq)(Keep.both).run()

    seqF.await.toSet shouldBe Set(Event.invalidEvent(eventKey2), distinctEvent1)
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_get_an_event_without_subscribing_for_it(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val event1   = makeDistinctEvent(301)
    val eventKey = event1.eventKey

    publisher.publish(event1).await
    Thread.sleep(500)

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
    Thread.sleep(500)

    val eventsF = subscriber.get(Set(eventKey1, eventKey2))
    eventsF.await shouldBe Set(Event.invalidEvent(eventKey2), event1)
  }
}
