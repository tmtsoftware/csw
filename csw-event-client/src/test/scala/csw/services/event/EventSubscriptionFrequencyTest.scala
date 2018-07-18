package csw.services.event

import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.stream.scaladsl.Sink
import csw.messages.events.{Event, EventKey, EventName}
import csw.services.event.api.scaladsl.SubscriptionModes
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.makeEventForKeyName
import csw.services.event.internal.kafka.KafkaTestProps
import csw.services.event.internal.redis.RedisTestProps
import csw.services.event.internal.wiring.BaseProperties
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatest.testng.TestNGSuite
import org.testng.annotations._

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.DurationLong
import scala.util.Random

class EventSubscriptionFrequencyTest extends TestNGSuite with Matchers with Eventually {
  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  var redisTestProps: RedisTestProps = _
  var kafkaTestProps: KafkaTestProps = _

  @BeforeSuite
  def beforeAll(): Unit = {
    redisTestProps = RedisTestProps.createRedisProperties(3566, 26384, 6384)
    kafkaTestProps = KafkaTestProps.createKafkaProperties(3567, 6004)
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

  @DataProvider(name = "redis-provider")
  def redisPubSubProvider: Array[Array[RedisTestProps]] = Array(
    Array(redisTestProps)
  )

  def events(name: EventName): immutable.Seq[Event] = for (i ← 1 to 1500) yield makeEventForKeyName(name, i)

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

  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_duration_with_rate_adapter_mode_for_slow_publisher(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val inbox = TestInbox[Event]()

    val eventGenerator = new EventGenerator(EventName(s"system_${Random.nextInt()}"))
    import eventGenerator._
    val eventKey: EventKey = eventsGroup.head.eventKey

    val cancellable = publisher.publish(eventGenerator.generator, 400.millis)
    Thread.sleep(500)
    val subscription =
      subscriber.subscribeActorRef(Set(eventKey), inbox.ref, 100.millis, SubscriptionModes.RateAdapterMode)
    Thread.sleep(1050)
    subscription.unsubscribe().await
    cancellable.cancel()

    val receivedEvents = inbox.receiveAll()
    receivedEvents.size shouldBe 11
    publishedEvents should contain allElementsOf receivedEvents
    // assert that received elements will have duplicates
    receivedEvents.toSet.size should not be 11
  }

  //DEOPSCSW-346: Subscribe to event irrespective of Publisher's existence
  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_an_event_with_duration_with_rate_adapter_for_fast_publisher(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._

    val receivedEvents: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val receivedEvents2: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventGenerator                        = new EventGenerator(EventName(s"system_${Random.nextInt()}"))
    import eventGenerator._
    val eventKey: EventKey = eventsGroup.head.eventKey

    val cancellable = publisher.publish(eventGenerator.generator, 100.millis)
    Thread.sleep(500)
    val subscription = subscriber
      .subscribe(Set(eventKey), 600.millis, SubscriptionModes.RateAdapterMode)
      .to(Sink.foreach[Event](receivedEvents.enqueue(_)))
      .run()
    subscription.ready().await

    val subscription2 = subscriber
      .subscribe(Set(eventKey), 800.millis, SubscriptionModes.RateAdapterMode)
      .to(Sink.foreach[Event](receivedEvents2.enqueue(_)))
      .run()
    subscription2.ready().await

    Thread.sleep(2000)

    subscription.unsubscribe().await
    subscription2.unsubscribe().await
    cancellable.cancel()

    receivedEvents.size shouldBe 4
    publishedEvents.size should be > receivedEvents.size
    publishedEvents should contain allElementsOf receivedEvents
    // assert if received elements do not have duplicates
    receivedEvents.toSet.size shouldBe 4

    receivedEvents2.size shouldBe 3
    publishedEvents.size should be > receivedEvents2.size
    publishedEvents should contain allElementsOf receivedEvents
    // assert if received elements do not have duplicates
    receivedEvents2.toSet.size shouldBe 3
  }

  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_duration_with_rate_limiter_mode_for_slow_publisher(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val inbox = TestInbox[Event]()

    val eventGenerator = new EventGenerator(EventName(s"system_${Random.nextInt()}"))
    import eventGenerator._
    val eventKey: EventKey = eventsGroup.head.eventKey

    val cancellable = publisher.publish(eventGenerator.generator, 400.millis)
    Thread.sleep(500)
    val subscription = subscriber.subscribeActorRef(Set(eventKey), inbox.ref, 100.millis, SubscriptionModes.RateLimiterMode)
    Thread.sleep(900)
    subscription.unsubscribe().await
    cancellable.cancel()

    val receivedEvents = inbox.receiveAll()
    receivedEvents.size shouldBe 3
    publishedEvents should contain allElementsOf receivedEvents
    // assert if received elements do not have duplicates
    receivedEvents.toSet.size shouldBe 3
  }

  //DEOPSCSW-342: Subscription with consumption frequency
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_subscribe_with_duration_with_rate_limiter_mode_for_fast_publisher(
      baseProperties: BaseProperties
  ): Unit = {
    import baseProperties._
    val inbox = TestInbox[Event]()

    val eventGenerator = new EventGenerator(EventName(s"system_${Random.nextInt()}"))
    import eventGenerator._
    val eventKey: EventKey = eventsGroup.head.eventKey

    val cancellable = publisher.publish(eventGenerator.generator, 100.millis)
    Thread.sleep(500)
    val subscription =
      subscriber.subscribeActorRef(Set(eventKey), inbox.ref, 400.millis, SubscriptionModes.RateLimiterMode)
    Thread.sleep(1800)
    subscription.unsubscribe().await
    cancellable.cancel()

    val receivedEvents = inbox.receiveAll()
    receivedEvents.size shouldBe 5
    publishedEvents should contain allElementsOf receivedEvents
    // assert if received elements do not have duplicates
    receivedEvents.toSet.size shouldBe 5
  }
}
