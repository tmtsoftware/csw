package csw.services.event

import akka.stream.scaladsl.{Keep, Sink}
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.events.{Event, EventKey, SystemEvent}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.makeDistinctEvent
import csw.services.event.internal.kafka.KafkaTestProps
import csw.services.event.internal.redis.RedisTestProps
import csw.services.event.internal.wiring._
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSuite, Matchers}
import org.testng.annotations._

import scala.concurrent.duration.DurationLong

class EventServiceTest extends FunSuite with Matchers with Eventually with EmbeddedKafka {

  var redisTestProps: RedisTestProps = _
  var kafkaTestProps: KafkaTestProps = _

  @BeforeSuite
  def beforeAll(): Unit = {
    redisTestProps = RedisTestProps.createRedisProperties(3566, 26384, 6384)
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
    Array(redisTestProps)
  )

  @Test(dataProvider = "event-service-provider")
  def should_be_able_to_publish_and_subscribe_an_event_through_eventService(): Unit = {
    val redisProps = redisTestProps
    import redisProps._

    val event1             = makeDistinctEvent(1)
    val eventKey: EventKey = event1.eventKey
    val testProbe          = TestProbe[Event]()(typedActorSystem)

    val subscription = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()

    subscription.ready.await
    publisher.publish(event1).await

    testProbe.expectMessageType[SystemEvent].isInvalid shouldBe true
    testProbe.expectMessage(event1)

    subscription.unsubscribe().await

    publisher.publish(event1).await
    testProbe.expectNoMessage(2.seconds)
  }
}
