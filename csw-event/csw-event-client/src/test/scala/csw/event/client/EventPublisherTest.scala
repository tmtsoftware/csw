package csw.event.client

import akka.actor.Cancellable
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.helpers.Utils.{makeDistinctEvent, makeEvent, makeEventWithPrefix}
import csw.event.client.internal.kafka.KafkaTestProps
import csw.event.client.internal.redis.{InitializationEvent, RedisTestProps}
import csw.event.client.internal.wiring._
import csw.params.core.models.{Prefix, Subsystem}
import csw.params.events.{Event, EventKey}
import csw.time.core.models.UTCTime
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatestplus.testng.TestNGSuite
import org.testng.annotations._

import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Random

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
  def pubSubProvider: Array[Array[_ <: BaseProperties]] = Array(
    Array(redisTestProps),
    Array(kafkaTestProps)
  )

  //DEOPSCSW-659: Investigate initial latency in event service pub sub API for single publish
  @Test
  def should_publish_initialization_event_on_publisher_creation(): Unit = {
    redisTestProps.publisher // access lazy publisher so that it gets evaulated
    val initEventKey = EventKey(s"${Subsystem.TEST}.init")
    val initEvent    = redisTestProps.subscriber.get(initEventKey).await
    initEvent.paramSet shouldBe InitializationEvent.value.paramSet
  }

  //DEOPSCSW-345: Publish events irrespective of subscriber existence
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_and_subscribe_an_event(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val event1             = makeDistinctEvent(Random.nextInt())
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(typedActorSystem)

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
  def should_be_able_to_publish_an_event_with_duration(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 1 to 10) yield makeEvent(i)

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
  def should_be_able_to_publish_concurrently_to_the_different_channel(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val events: immutable.Seq[Event] = for (i ← 101 to 110) yield makeDistinctEvent(i)

    val subscription = subscriber.subscribe(events.map(_.eventKey).toSet).to(Sink.foreach(queue.enqueue(_))).run()
    subscription.ready().await

    publisher.publish(Source.fromIterator(() ⇒ events.toIterator))

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 20)

    queue should contain theSameElementsAs events.map(x ⇒ Event.invalidEvent(x.eventKey)) ++ events
  }

  //DEOPSCSW-000: Publish events with block generating future of event
  //DEOPSCSW-516: Optionally Publish - API Change
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_an_event_with_block_generating_future_of_event(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 31 to 41) yield makeEventWithPrefix(i, Prefix("test"))

    def eventGenerator(): Future[Option[Event]] = Future {
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
  def should_be_able_to_maintain_ordering_while_publish(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val prefix             = Prefix("ordering.test.prefix")
    val event1             = makeEventWithPrefix(1, prefix)
    val event2             = makeEventWithPrefix(2, prefix)
    val event3             = makeEventWithPrefix(3, prefix)
    val event4             = makeEventWithPrefix(4, prefix)
    val event5             = makeEventWithPrefix(5, prefix)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(typedActorSystem)

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
  def should_be_able_to_publish_event_via_event_generator_with_start_time(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 31 to 41) yield makeEventWithPrefix(i, Prefix("test.publish"))

    def eventGenerator(): Option[Event] = {
      counter += 1
      if (counter > 1) None
      else Some(events(counter))
    }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = events.head.eventKey

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.ready().await
    Thread.sleep(500) // Needed for getting the latest event

    val cancellable = publisher.publish(eventGenerator(), UTCTime(UTCTime.now().value.plusSeconds(1)), 300.millis)

    Thread.sleep(2000)
    cancellable.cancel()

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 4 published events will follow
    queue should (have length 3 and contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events.take(2))
  }

  //DEOPSCSW-515: Include Start Time in API
  //DEOPSCSW-516: Optionally Publish - API Change
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_event_via_asynchronous_event_generator_with_start_time(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 31 to 41) yield makeEventWithPrefix(i, Prefix("test.publishAsync"))

    def eventGenerator(): Future[Option[Event]] = Future {
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
}
