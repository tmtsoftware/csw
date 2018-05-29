package csw.services.event

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.events.{Event, EventKey}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.{makeDistinctEvent, makeEvent}
import csw.services.event.internal.kafka.KafkaTestProps
import csw.services.event.internal.redis.RedisTestProps
import csw.services.event.internal.wiring._
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatest.testng.TestNGSuite
import org.testng.annotations._

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.DurationLong

//DEOPSCSW-334: Publish an event
//DEOPSCSW-335: Model for EventName that encapsulates the topic(or channel ) name
//DEOPSCSW-337: Subscribe to an event based on prefix
class EventPublisherTest extends TestNGSuite with Matchers with Eventually with EmbeddedKafka {

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 10.millis)

  var redisTestProps: RedisTestProps = _
  var kafkaTestProps: KafkaTestProps = _

  @BeforeSuite
  def beforeAll(): Unit = {
    redisTestProps = RedisTestProps.createRedisProperties(3561, 26381, 6381)
    kafkaTestProps = KafkaTestProps.createKafkaProperties(3562, 6002)
    redisTestProps.redisSentinel.start()
    redisTestProps.redis.start()
    EmbeddedKafka.start()(kafkaTestProps.config)
  }

  @AfterSuite
  def afterAll(): Unit = {
    redisTestProps.redisClient.shutdown()
    redisTestProps.redis.stop()
    redisTestProps.redisSentinel.stop()
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

  //DEOPSCSW-345: Publish events irrespective of subscriber existence
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_and_subscribe_an_event(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

    val event1             = makeDistinctEvent(1)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(actorSystem.toTyped)

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
    import baseProperties.wiring._

    var counter                      = -1
    val events: immutable.Seq[Event] = for (i ← 1 to 10) yield makeEvent(i)

    def eventGenerator(): Event = {
      counter += 1
      events(counter)
    }

    val queue: mutable.Queue[Event] = new mutable.Queue[Event]()
    val eventKey: EventKey          = makeEvent(0).eventKey

    val subscription = subscriber.subscribe(Set(eventKey)).to(Sink.foreach[Event](queue.enqueue(_))).run()
    subscription.ready.await

    cancellable = publisher.publish(eventGenerator, 2.millis)
    Thread.sleep(1000)
    cancellable.cancel()

    // subscriber will receive an invalid event first as subscription happened before publishing started.
    // The 10 published events will follow
    eventually(queue.size shouldBe 11)

    queue should contain allElementsOf Seq(Event.invalidEvent(eventKey)) ++ events
  }

  //DEOPSCSW-341: Allow to reuse single connection for subscribing to multiple EventKeys
  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_concurrently_to_the_different_channel(baseProperties: BaseProperties): Unit = {
    import baseProperties._
    import baseProperties.wiring._

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
}
