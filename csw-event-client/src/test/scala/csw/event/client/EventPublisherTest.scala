package csw.event.client

import akka.actor.Cancellable
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.helpers.Utils.{makeDistinctEvent, makeEvent, makeEventWithPrefix}
import csw.event.client.internal.kafka.KafkaTestProps
import csw.event.client.internal.redis.RedisTestProps
import csw.event.client.internal.wiring._
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventKey}
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
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel) name
//DEOPSCSW-337: Subscribe to an event based on prefix
//DEOPSCSW-349: Event Service API creation
//DEOPSCSW-395: Provide EventService handle to component developers
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
  var cancellable: Cancellable = _
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_an_event_with_duration(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 1 to 10) yield makeEvent(i)

    def eventGenerator(): Event = {
      counter += 1
      events(counter)
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
    queue should (have length 5 and contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events.take(4))
  }

  //DEOPSCSW-341: Allow to reuse single connection for subscribing to multiple EventKeys
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_concurrently_to_the_different_channel(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    val queue: mutable.Queue[Event]  = new mutable.Queue[Event]()
    val events: immutable.Seq[Event] = for (i ← 101 to 110) yield makeDistinctEvent(i)

    val subscription = subscriber.subscribe(events.map(_.eventKey).toSet).to(Sink.foreach(queue.enqueue(_))).run()
    subscription.ready.await

    publisher.publish(Source.fromIterator(() ⇒ events.toIterator))

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 20)

    queue should contain theSameElementsAs events.map(x ⇒ Event.invalidEvent(x.eventKey)) ++ events
  }

  //DEOPSCSW-000: Publish events with block generating futre of event
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_an_event_with_block_genrating_future_of_event(baseProperties: BaseProperties): Unit = {
    import baseProperties._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 31 to 41) yield makeEventWithPrefix(i, Prefix("test"))

    def eventGenerator(): Future[Event] = Future {
      counter += 1
      events(counter)
    }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = events(0).eventKey

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.ready().await
    Thread.sleep(500) // Needed for getting the latest event

    val cancellable = publisher.publishAsync(eventGenerator(), 300.millis)
    Thread.sleep(1000)
    cancellable.cancel()

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 4 published events will follow
    queue should (have length 5 and contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events.take(4))
  }

}
