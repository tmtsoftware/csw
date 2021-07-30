package csw.event.client

import akka.actor.testkit.typed.scaladsl.{TestInbox, TestProbe}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Keep, Sink}
import csw.event.api.scaladsl.SubscriptionModes
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.helpers.Utils._
import csw.prefix.models.{Prefix, Subsystem}
import csw.event.client.internal.kafka.KafkaTestProps
import csw.event.client.internal.redis.RedisTestProps
import csw.event.client.internal.wiring.BaseProperties
import csw.params.core.models.ObsId
import csw.params.events.{Event, EventKey, EventName, IRDetectorEvent, OpticalDetectorEvent, SystemEvent, WFSDetectorEvent}
import org.scalatest.concurrent.Eventually
import org.scalatestplus.testng.TestNGSuite
import org.testng.annotations._

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Random
import org.scalatest.matchers.should.Matchers

//DEOPSCSW-331: Event Service Accessible to all CSW component builders
//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
//DEOPSCSW-349: Event Service API creation
//DEOPSCSW-395: Provide EventService handle to component developers
class EventSubscriberTest extends TestNGSuite with Matchers with Eventually {

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  var redisTestProps: RedisTestProps = _
  var kafkaTestProps: KafkaTestProps = _

  @BeforeSuite
  def beforeAll(): Unit = {
    redisTestProps = RedisTestProps.createRedisProperties()
    kafkaTestProps = KafkaTestProps.createKafkaProperties()
    redisTestProps.start()
    kafkaTestProps.start()
  }

  @AfterSuite
  def afterAll(): Unit = {
    redisTestProps.shutdown()
    kafkaTestProps.shutdown()
  }

  @DataProvider(name = "event-service-provider")
  def pubSubProvider: Array[Array[_ <: BaseProperties]] =
    Array(
      Array(redisTestProps),
      Array(kafkaTestProps)
    )

  @DataProvider(name = "redis-provider")
  def redisPubSubProvider: Array[Array[RedisTestProps]] = Array(Array(redisTestProps))

  val events: immutable.Seq[Event]                  = for (i <- 1 to 1500) yield makeEvent(i)
  def events(name: EventName): immutable.Seq[Event] = for (i <- 1 to 1500) yield makeEventForKeyName(name, i)

  class EventGenerator(eventName: EventName) {
    var counter                               = 0
    var publishedEvents: mutable.Queue[Event] = mutable.Queue.empty
    val eventsGroup: immutable.Seq[Event]     = events(eventName)

    def generator: Event = {
      counter += 1
      val event = eventsGroup(counter)
      publishedEvents.enqueue(event)
      event
    }
  }

  var eventId = 0
  def eventGenerator(): Option[Event] =
    Option {
      eventId += 1
      events(eventId)
    }

  //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
  //DEOPSCSW-343: Unsubscribe based on prefix and event name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_an_event__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_346_DEOPSCSW_343(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val event1             = makeDistinctEvent(Random.nextInt())
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()
    val subscription       = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()

    subscription.ready().await
    testProbe.expectMessageType[SystemEvent].isInvalid shouldBe true

    publisher.publish(event1).await
    testProbe.expectMessage(event1)

    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(2.seconds)
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  //DEOPSCSW-343: Unsubscribe based on prefix and event name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_async_callback__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338_DEOPSCSW_343(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val event1    = makeEvent(1)
    val testProbe = TestProbe[Event]()

    val callback: Event => Future[Event] = event => Future.successful(testProbe.ref ! event).map(_ => event)(ec)

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
  def should_be_able_to_subscribe_with_async_callback_with_duration__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338_DEOPSCSW_342(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val queue2: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey                     = events.head.eventKey

    val callback: Event => Future[Unit]  = event => Future.successful(queue.enqueue(event))
    val callback2: Event => Future[Unit] = event => Future.successful(queue2.enqueue(event))
    eventId = 0
    val cancellable = publisher.publish(eventGenerator(), 1.millis)

    val subscription  = subscriber.subscribeAsync(Set(eventKey), callback, 300.millis, SubscriptionModes.RateAdapterMode)
    val subscription2 = subscriber.subscribeAsync(Set(eventKey), callback2, 400.millis, SubscriptionModes.RateAdapterMode)
    Thread.sleep(1000) // Future in callback needs time to execute
    subscription.unsubscribe().await
    subscription2.unsubscribe().await

    cancellable.cancel()
    queue.size shouldBe 4
    queue2.size shouldBe 3
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_callback__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338_DEOPSCSW_346(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val listOfPublishedEvents: ArrayBuffer[Event] = ArrayBuffer.empty

    val testProbe               = TestProbe[Event]()
    val callback: Event => Unit = testProbe.ref ! _

    // all events are published to same topic with different id's
    (1 to 5).foreach { id =>
      val event = makeEvent(id)
      listOfPublishedEvents += event
      publisher.publish(event).await
    }

    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val subscription = subscriber.subscribeCallback(Set(listOfPublishedEvents.head.eventKey), callback)
    testProbe.expectMessage(listOfPublishedEvents.last)
    subscription.unsubscribe().await

    publisher.publish(listOfPublishedEvents.last).await
    testProbe.expectNoMessage(200.millis)
  }

  //DEOPSCSW-338: Provide callback for Event alerts
  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_callback_with_duration__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_338_DEOPSCSW_342(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val queue2: mutable.Queue[Event] = new mutable.Queue[Event]()
    val event1                       = makeEvent(1)

    val callback: Event => Unit  = queue.enqueue(_)
    val callback2: Event => Unit = queue2.enqueue(_)
    eventId = 0
    val cancellable = publisher.publish(eventGenerator(), 1.millis)
    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val subscription = subscriber.subscribeCallback(Set(event1.eventKey), callback, 300.millis, SubscriptionModes.RateAdapterMode)
    val subscription2 =
      subscriber.subscribeCallback(Set(event1.eventKey), callback2, 400.millis, SubscriptionModes.RateAdapterMode)
    Thread.sleep(1000)
    subscription.unsubscribe().await
    subscription2.unsubscribe().await

    cancellable.cancel()
    queue.size shouldBe 4
    queue2.size shouldBe 3
  }

  //DEOPSCSW-339: Provide actor ref to alert about Event arrival
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_an_ActorRef__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_339(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val event1 = makeDistinctEvent(Random.nextInt())

    val probe = TestProbe[Event]()

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
  def should_be_able_to_subscribe_with_an_ActorRef_with_duration__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_339_DEOPSCSW_342(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val inbox  = TestInbox[Event]()
    val event1 = makeEvent(205)

    publisher.publish(event1).await
    Thread.sleep(1000) // Needed for redis set which is fire and forget operation
    val subscription =
      subscriber.subscribeActorRef(Set(event1.eventKey), inbox.ref, 300.millis, SubscriptionModes.RateAdapterMode)
    Thread.sleep(1000)
    subscription.unsubscribe().await

    inbox.receiveAll().size shouldBe 4
  }

  // DEOPSCSW-420: Implement Pattern based subscription
  // Pattern subscription doesn't work with embedded kafka hence not running it with the suite
  @Test(dataProvider = "redis-provider")
  def should_be_able_to_subscribe_an_event_with_pattern_from_different_subsystem__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_420(
      redisProps: RedisTestProps
  ): Unit = {
    import redisProps._

    val testEvent1 = makeEventWithPrefix(1, Prefix("csw.prefix"))
    val testEvent2 = makeEventWithPrefix(2, Prefix("csw.prefix"))
    val tcsEvent1  = makeEventWithPrefix(1, Prefix("tcs.prefix"))
    val testProbe  = TestProbe[Event]()

    // pattern is * for redis
    val subscription = subscriber.pSubscribeCallback(Subsystem.CSW, eventPattern, testProbe.ref ! _)
    subscription.ready().await
    Thread.sleep(500)

    publisher.publish(testEvent1).await
    publisher.publish(testEvent2).await

    testProbe.expectMessage(testEvent1)
    testProbe.expectMessage(testEvent2)

    publisher.publish(tcsEvent1).await
    testProbe.expectNoMessage(2.seconds)

    subscription.unsubscribe().await
  }

  // DEOPSCSW-420: Implement Pattern based subscription
  // Pattern subscription doesn't work with embedded kafka hence not running it with the suite
  @Test(dataProvider = "redis-provider")
  def should_be_able_to_subscribe_an_event_with_pattern_from_same_subsystem__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_420(
      redisProps: RedisTestProps
  ): Unit = {
    import redisProps._

    val testEvent1 = makeEventForKeyName(EventName("movement.linear"), 1)
    val testEvent2 = makeEventForKeyName(EventName("movement.angular"), 2)
    val testEvent3 = makeEventForKeyName(EventName("temperature"), 3)
    val testEvent4 = makeEventForKeyName(EventName("move"), 4)
    val testEvent5 = makeEventForKeyName(EventName("cove"), 5)
    val testEvent6 = makeEventForPrefixAndKeyName(Prefix("csw.test_prefix"), EventName("move"), 6)

    val inbox  = TestInbox[Event]()
    val inbox2 = TestInbox[Event]()
    val inbox3 = TestInbox[Event]()
    val inbox4 = TestInbox[Event]()
    val inbox5 = TestInbox[Event]()

    val eventPattern  = "*.movement.*" //subscribe to events with any prefix but event name containing 'movement'
    val eventPattern2 = "*.move*"      //subscribe to events with any prefix but event name containing 'move'
    val eventPattern3 =
      "*.?ove" //subscribe to events with any prefix but event name matching any first  character followed by `ove`
    val eventPattern4 = "test_prefix.*" //subscribe to all events with prefix `test_prefix` irresepective of event names
    val eventPattern5 = "*"             //subscribe to all events with prefix `test_prefix` irresepective of event names

    val subscription  = subscriber.pSubscribeCallback(Subsystem.CSW, eventPattern, inbox.ref ! _)
    val subscription2 = subscriber.pSubscribeCallback(Subsystem.CSW, eventPattern2, inbox2.ref ! _)
    val subscription3 = subscriber.pSubscribeCallback(Subsystem.CSW, eventPattern3, inbox3.ref ! _)
    val subscription4 = subscriber.pSubscribeCallback(Subsystem.CSW, eventPattern4, inbox4.ref ! _)
    val subscription5 = subscriber.pSubscribeCallback(Subsystem.CSW, eventPattern5, inbox5.ref ! _)

    subscription.ready().await
    subscription2.ready().await
    subscription3.ready().await
    subscription4.ready().await
    subscription5.ready().await

    Thread.sleep(500)

    publisher.publish(testEvent1).await
    publisher.publish(testEvent2).await
    publisher.publish(testEvent3).await
    publisher.publish(testEvent4).await
    publisher.publish(testEvent5).await
    publisher.publish(testEvent6).await

    Thread.sleep(1000)

    val receivedEvents  = inbox.receiveAll()
    val receivedEvents2 = inbox2.receiveAll()
    val receivedEvents3 = inbox3.receiveAll()
    val receivedEvents4 = inbox4.receiveAll()
    val receivedEvents5 = inbox5.receiveAll()

    (receivedEvents should contain).only(testEvent1, testEvent2)
    (receivedEvents2 should contain).only(testEvent1, testEvent2, testEvent4, testEvent6)
    (receivedEvents3 should contain).only(testEvent4, testEvent5, testEvent6)
    (receivedEvents4 should contain).only(testEvent6)
    (receivedEvents5 should contain).allOf(testEvent1, testEvent2, testEvent3, testEvent4, testEvent5, testEvent6)

    subscription.unsubscribe().await
    subscription2.unsubscribe().await
    subscription3.unsubscribe().await
    subscription4.unsubscribe().await
    subscription5.unsubscribe().await
  }

  // Pattern subscription doesn't work with embedded kafka hence not running it with the suite
  @Test(dataProvider = "redis-provider")
  def should_be_able_to_subscribe_all_observe_events__CSW_119(redisProps: RedisTestProps): Unit = {
    import redisProps._

    val obsId          = ObsId("2020A-001-123")
    val irDetObsStart  = IRDetectorEvent.observeStart(Prefix("IRIS.det"), obsId)
    val irDetObsEnd    = IRDetectorEvent.observeEnd(Prefix("IRIS.det"), obsId)
    val publishSuccess = WFSDetectorEvent.publishSuccess(Prefix("WFOS.test"))
    val optDetObsStart = OpticalDetectorEvent.observeStart(Prefix("WFOS.det"), obsId)

    val testEvent = makeEventWithPrefix(1, Prefix("csw.prefix"))
    val buffer    = mutable.ArrayBuffer.empty[Event]

    val subscription = subscriber
      .subscribeObserveEvents()
      .wireTap(e => buffer.addOne(e))
      .toMat(Sink.ignore)(Keep.left)
      .run()
    subscription.ready().await
    Thread.sleep(500)

    publisher.publish(irDetObsStart).await
    publisher.publish(irDetObsEnd).await
    publisher.publish(testEvent).await
    publisher.publish(publishSuccess).await
    publisher.publish(optDetObsStart).await
    // verify all the obs events received except testEvent
    eventually(buffer.toList shouldBe List(irDetObsStart, irDetObsEnd, publishSuccess, optDetObsStart))

    subscription.unsubscribe().await
  }

  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_make_independent_subscriptions__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val prefix        = Prefix("csw.prefix")
    val eventName1    = EventName("system1")
    val eventName2    = EventName("system2")
    val event1: Event = SystemEvent(prefix, eventName1)
    val event2: Event = SystemEvent(prefix, eventName2)

    val (subscription, seqF) = subscriber.subscribe(Set(event1.eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription.ready().await
    Thread.sleep(200)
    publisher.publish(event1).await

    val (subscription2, seqF2) = subscriber.subscribe(Set(event2.eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription2.ready().await
    Thread.sleep(200)
    publisher.publish(event2).await

    seqF.await.toSet shouldBe Set(Event.invalidEvent(event1.eventKey), event1)
    seqF2.await.toSet shouldBe Set(Event.invalidEvent(event2.eventKey), event2)
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_retrieve_recently_published_event_on_subscription__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_340(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val event1   = makeEvent(1)
    val event2   = makeEvent(2)
    val event3   = makeEvent(3)
    val eventKey = event1.eventKey

    publisher.publish(event1).await
    publisher.publish(event2).await // latest event before subscribing

    Thread.sleep(500) // Needed for redis set which is fire and forget operation
    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription.ready().await
    Thread.sleep(500) // Needed for getting the latest event

    publisher.publish(event3).await

    // assertion against a sequence ensures that the latest event before subscribing arrives earlier in the stream
    seqF.await shouldBe Seq(event2, event3)
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_retrieve_InvalidEvent__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_340(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val eventKey = EventKey("csw.a.b.c")

    val (_, seqF) = subscriber.subscribe(Set(eventKey)).take(1).toMat(Sink.seq)(Keep.both).run()

    seqF.await shouldBe Seq(Event.invalidEvent(eventKey))
  }

  //DEOPSCSW-340: Provide most recently published event for subscribed prefix and name
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_retrieve_valid_as_well_as_invalid_event_when_events_are_published_for_some_and_not_for_other_keys__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_340(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val distinctEvent1 = makeDistinctEvent(Random.nextInt())
    val distinctEvent2 = makeDistinctEvent(Random.nextInt())

    val eventKey1 = distinctEvent1.eventKey
    val eventKey2 = distinctEvent2.eventKey

    publisher.publish(distinctEvent1).await
    Thread.sleep(500)

    val (_, seqF) = subscriber.subscribe(Set(eventKey1, eventKey2)).take(2).toMat(Sink.seq)(Keep.both).run()

    seqF.await.toSet shouldBe Set(Event.invalidEvent(eventKey2), distinctEvent1)
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_get_an_event_without_subscribing_for_it__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_344(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val event1   = makeDistinctEvent(Random.nextInt())
    val eventKey = event1.eventKey

    publisher.publish(event1).await
    Thread.sleep(500)

    val eventF = subscriber.get(eventKey)
    eventF.await shouldBe event1
  }

  //DEOPSCSW-344: Retrieve recently published event using prefix and eventname
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_get_InvalidEvent__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_344(
      baseProperties: BaseProperties
  ): Unit = {
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
  def should_be_able_to_get_events_for_multiple_event_keys__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_344(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val event1    = makeDistinctEvent(Random.nextInt())
    val eventKey1 = event1.eventKey

    val event2    = makeDistinctEvent(Random.nextInt())
    val eventKey2 = event2.eventKey

    publisher.publish(event1).await
    Thread.sleep(500)

    val eventsF = subscriber.get(Set(eventKey1, eventKey2))
    eventsF.await shouldBe Set(Event.invalidEvent(eventKey2), event1)
  }

  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_get_invalid_event_on_event_parse_failure__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val eventKey = makeEvent(0).eventKey

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).take(2).toMat(Sink.seq)(Keep.both).run()
    subscription.ready().await
    Thread.sleep(500)

    publishGarbage(eventKey.key, "garbage").await
    Thread.sleep(200)
    subscription.unsubscribe()

    (seqF.await should contain).inOrder(Event.invalidEvent(eventKey), Event.badEvent())
  }

  //CSW-73: Make event pub/sub resuming
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_resume_subscriber_after_exception__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val event1                      = makeEvent(1)

    val cancellable = publisher.publish(eventGenerator(), 1.millis)
    Thread.sleep(500) // Needed for redis set which is fire and forget operation

    val subscription =
      subscriber.subscribeCallback(Set(event1.eventKey), resumingCallback(queue), 200.millis, SubscriptionModes.RateAdapterMode)
    Thread.sleep(1000)
    subscription.unsubscribe().await

    queue.size > 1 shouldBe true
    cancellable.cancel()
  }

  //CSW-73: Make event pub/sub resuming
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_resume_async_subscriber_after_exception__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395(
      baseProperties: BaseProperties
  ): Unit = {
    eventId = 0
    import baseProperties._
    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val event1                      = makeEvent(1)

    val cancellable = publisher.publish(eventGenerator(), 1.millis)
    Thread.sleep(500) // Needed for redis set which is fire and forget operation

    val subscription2 = subscriber.subscribeAsync(
      Set(event1.eventKey),
      resumingAsyncCallback(queue),
      200.millis,
      SubscriptionModes.RateAdapterMode
    )
    Thread.sleep(1000)
    subscription2.unsubscribe().await
    queue.size > 1 shouldBe true

    cancellable.cancel()
  }

  //CSW-73: Make event pub/sub resuming
  @Test(dataProvider = "redis-provider")
  def should_be_able_to_resume_pattern_subscriber_after_exception__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395(
      redisProps: RedisTestProps
  ): Unit = {
    import redisProps._
    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()

    val cancellable = publisher.publish(eventGenerator(), 1.millis)
    Thread.sleep(500) // Needed for redis set which is fire and forget operation

    val subscription = subscriber.pSubscribeCallback(Subsystem.CSW, eventPattern, resumingCallback(queue))
    Thread.sleep(1000)
    subscription.unsubscribe().await
    queue.size > 2 shouldBe true

    cancellable.cancel()
  }

  private def resumingCallback(queue: mutable.Queue[Event]) = {
    var counter = 0
    val callback: Event => Unit = event => {
      counter += 1
      if (counter % 2 == 0) {
        throw new RuntimeException("shouldResumeAfterThisException")
      }
      queue.enqueue(event)
    }
    callback
  }

  private def resumingAsyncCallback(queue: mutable.Queue[Event]) = {
    val system = ActorSystem(Behaviors.empty, "test")
    import system.executionContext
    val callback: Event => Future[Unit] = (event: Event) => Future(resumingCallback(queue)(event))
    callback
  }

}
