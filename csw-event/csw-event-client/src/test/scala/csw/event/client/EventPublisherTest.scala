package csw.event.client

import akka.actor.Cancellable
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.helpers.Utils.{makeDistinctEvent, makeEvent, makeEventWithPrefix}
import csw.params.core.generics.{Key, Parameter}
import csw.params.core.generics.KeyType.ByteKey
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.{Prefix, Subsystem}
import csw.event.client.internal.kafka.KafkaTestProps
import csw.event.client.internal.redis.{InitializationEvent, RedisTestProps}
import csw.event.client.internal.wiring._
import csw.params.events.{Event, EventKey}
import csw.time.core.models.UTCTime
import io.github.embeddedkafka.EmbeddedKafka
import org.scalatest.concurrent.Eventually
import org.scalatestplus.testng.TestNGSuite
import org.testng.annotations._

import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Random
import org.scalatest.matchers.should.Matchers

//DEOPSCSW-331: Event Service Accessible to all CSW component builders
//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel) name
//DEOPSCSW-337: Subscribe to an event based on prefix
//DEOPSCSW-349: Event Service API creation
//DEOPSCSW-395: Provide EventService handle to component developers
//DEOPSCSW-515: Include Start Time in API
//DEOPSCSW-516: Optionally Publish - API Change
class EventPublisherTest extends TestNGSuite with Matchers with Eventually with EmbeddedKafka {

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

  //DEOPSCSW-659: Investigate initial latency in event service pub sub API for single publish
  @Test
  def should_publish_initialization_event_on_publisher_creation__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516_DEOPSCSW_659()
      : Unit = {
    redisTestProps.publisher // access lazy publisher so that it gets evaluated
    val initEventKey = EventKey(s"${Subsystem.CSW}.first.event.init")
    val initEvent    = redisTestProps.subscriber.get(initEventKey).await
    initEvent.paramSet shouldBe InitializationEvent.value.paramSet
  }

  //DEOPSCSW-345: Publish events irrespective of subscriber existence
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_and_subscribe_an_event__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516_DEOPSCSW_345(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val event1             = makeDistinctEvent(Random.nextInt())
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()

    publisher.publish(event1).await
    Thread.sleep(500) // Needed for redis set which is fire and forget operation

    val subscription = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()
    subscription.ready().await

    testProbe.expectMessage(event1)

    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(2.seconds)
  }

  //DEOPSCSW-345: Publish events irrespective of subscriber existence
  //DEOPSCSW-516: Optionally Publish - API Change
  var cancellable: Cancellable = _
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_an_event_with_duration__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516_DEOPSCSW_345(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i <- 1 to 10) yield makeEvent(i)

    def eventGenerator(): Option[Event] = {
      counter += 1
      if (counter > 1) None
      else Some(events(counter))
    }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = makeEvent(0).eventKey

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.ready().await
    Thread.sleep(500) // Needed for getting the latest event

    cancellable = publisher.publish(eventGenerator(), 300.millis)
    Thread.sleep(1000)
    cancellable.cancel()

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 4 published events will follow
    queue should (have length 3 and contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events.take(2))
  }

  //DEOPSCSW-341: Allow to reuse single connection for subscribing to multiple EventKeys
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_concurrently_to_the_different_channel__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516_DEOPSCSW_341(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val events: immutable.Seq[Event] = for (i <- 101 to 110) yield makeDistinctEvent(i)

    val subscription = subscriber.subscribe(events.map(_.eventKey).toSet).to(Sink.foreach(queue.enqueue(_))).run()
    subscription.ready().await

    publisher.publish(Source.fromIterator(() => events.iterator))

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 20)

    queue should contain theSameElementsAs events.map(x => Event.invalidEvent(x.eventKey)) ++ events
  }

  //DEOPSCSW-000: Publish events with block generating future of event
  //DEOPSCSW-516: Optionally Publish - API Change
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_an_event_with_block_generating_future_of_event__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i <- 31 to 41) yield makeEventWithPrefix(i, Prefix("csw.move"))

    def eventGenerator(): Future[Option[Event]] =
      Future {
        counter += 1
        if (counter > 1) None
        else Some(events(counter))
      }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = events.head.eventKey

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.ready().await
    Thread.sleep(500) // Needed for getting the latest event

    val cancellable = publisher.publishAsync(eventGenerator(), 300.millis)
    Thread.sleep(1000)
    cancellable.cancel()

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 4 published events will follow
    queue should (have length 3 and contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events.take(2))
  }

  //DEOPSCSW-595: Enforce ordering in publish
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_maintain_ordering_while_publish__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516_DEOPSCSW_595(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val prefix             = Prefix("csw.ordering.prefix")
    val event1             = makeEventWithPrefix(1, prefix)
    val event2             = makeEventWithPrefix(2, prefix)
    val event3             = makeEventWithPrefix(3, prefix)
    val event4             = makeEventWithPrefix(4, prefix)
    val event5             = makeEventWithPrefix(5, prefix)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()

    val subscription = subscriber
      .subscribe(Set(eventKey))
      .toMat(Sink.foreach[Event](testProbe.ref ! _))(Keep.left)
      .run()

    subscription.ready().await
    Thread.sleep(500)

    publisher.publish(event1)
    publisher.publish(event2)
    publisher.publish(event3)
    publisher.publish(event4)
    publisher.publish(event5)

    testProbe.expectMessage(Event.invalidEvent(eventKey))
    testProbe.expectMessage(event1)
    testProbe.expectMessage(event2)
    testProbe.expectMessage(event3)
    testProbe.expectMessage(event4)
    testProbe.expectMessage(event5)
  }

  //DEOPSCSW-515: Include Start Time in API
  //DEOPSCSW-516: Optionally Publish - API Change
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_event_via_event_generator_with_start_time__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val eventKey: EventKey = EventKey("csw.publish.system")
    var counter            = 0
    // Queue for events of event generator
    val generatorEvents = mutable.Queue[Event]()

    // Generator produces no event when counter remainder is 0, so publishes events: 1, 3, 5...
    // Therefore every other event is optionally published
    def eventGenerator(): Option[Event] = {
      counter += 1
      if (counter % 2 == 0) None
      else {
        val event: Event = makeEventWithPrefix(counter, Prefix("csw.publish"))
        // Save the events as they are created for later test
        generatorEvents.enqueue(event)
        Some(event)
      }
    }

    // Queue for events that are gathered by subscriber
    val queue: mutable.Queue[Event] = mutable.Queue[Event]()

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.ready().await
    Thread.sleep(500) // Needed for getting the initial invalid event

    // Should have the invalid event at this point
    queue.length shouldBe 1

    // Save for later comparison
    val startTime: UTCTime = UTCTime(UTCTime.now().value.plusSeconds((1)))
    // Now start publishing after
    val cancellable = publisher.publish(eventGenerator(), startTime, 200.millis)

    // 1 second delay, 0=None, 1, 2=None, 3, 4=None, 5 -> total of ~2000 ms
    Thread.sleep(2100)
    cancellable.cancel()

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 3 published events will follow invalid
    queue should (have length 4 and contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ generatorEvents.take(3))
    // Remove the invalid and verify that the events are later than start time
    queue.tail.foreach(ev => ev.eventTime should be >= startTime)
  }

  //DEOPSCSW-515: Include Start Time in API
  //DEOPSCSW-516: Optionally Publish - API Change
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_event_via_asynchronous_event_generator_with_start_time__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i <- 31 to 41) yield makeEventWithPrefix(i, Prefix("csw.publishAsync"))

    def eventGenerator(): Future[Option[Event]] =
      Future {
        counter += 1
        if (counter > 1) None
        else Some(events(counter))
      }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = events.head.eventKey

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.ready().await
    Thread.sleep(500) // Needed for getting the latest event

    val cancellable = publisher.publishAsync(
      eventGenerator(),
      UTCTime(UTCTime.now().value.plusSeconds(1)),
      300.millis
    )
    Thread.sleep(2000)
    cancellable.cancel()

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 4 published events will follow
    queue should (have length 3 and contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events.take(2))
  }

  @Test(dataProvider = "event-service-provider")
  def large_event_test__DEOPSCSW_331_DEOPSCSW_334_DEOPSCSW_335_DEOPSCSW_337_DEOPSCSW_349_DEOPSCSW_395_DEOPSCSW_515_DEOPSCSW_516(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val payloadKey: Key[Byte]       = ByteKey.make("payloadKey")
    val payload: Array[Byte]        = ("0" * 1024 * 2).getBytes("utf-8")
    val paramSet: Set[Parameter[_]] = Set(payloadKey.setAll(payload))
    val event1                      = SystemEvent(Prefix("csw.abc"), EventName("system_1"), paramSet)

    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()
    val subscription       = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()
    subscription.ready().await

    publisher.publish(event1).await
    testProbe.expectMessageType[SystemEvent]
  }
}
