//package csw.services.event
//
//import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
//import akka.stream.scaladsl.{Keep, Sink}
//import akka.testkit.typed.scaladsl.TestProbe
//import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
//import csw.messages.events.{Event, EventKey, SystemEvent}
//import csw.services.event.helpers.TestFutureExt.RichFuture
//import csw.services.event.helpers.Utils.makeDistinctEvent
//import csw.services.event.internal.redis.RedisTestProps
//import csw.services.event.internal.wiring._
//import net.manub.embeddedkafka.EmbeddedKafka
//import org.scalatest.concurrent.Eventually
//import org.scalatest.{FunSuite, Matchers}
//import org.testng.annotations._
//
//import scala.concurrent.duration.DurationLong
//
//class EventServiceTest extends FunSuite with Matchers with Eventually with EmbeddedKafka {
//
//  var redisTestProps: RedisTestProps = _
//
//  @BeforeSuite
//  def beforeAll(): Unit = {
//    redisTestProps = RedisTestProps.createRedisProperties(3566, 26384, 6384)
//    redisTestProps.redisSentinel.start()
//    redisTestProps.redis.start()
//  }
//
//  @AfterSuite
//  def afterAll(): Unit = {
//    redisTestProps.redisClient.shutdown()
//    redisTestProps.redis.stop()
//    redisTestProps.redisSentinel.stop()
//    redisTestProps.wiring.shutdown(TestFinishedReason).await
//  }
//
//  @DataProvider(name = "event-service-provider")
//  def pubSubProvider: Array[Array[_ <: BaseProperties]] = Array(
//    Array(redisTestProps)
//  )
//
//  @Test(dataProvider = "event-service-provider")
//  def should_be_able_to_publish_and_subscribe_an_event_through_eventService(): Unit = {
//    val redisProps = redisTestProps
//    import redisProps._
//    import redisProps.wiring._
//
//    val event1             = makeDistinctEvent(1)
//    val eventKey: EventKey = event1.eventKey
//    val testProbe          = TestProbe[Event]()(actorSystem.toTyped)
//    val subscriber         = eventService.subscriber
//    val publisher          = eventService.defaultPublisher
//
//    val subscription = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(testProbe.ref ! _))(Keep.left).run()
//
//    subscription.ready.await
//    publisher.publish(event1).await
//
//    testProbe.expectMessageType[SystemEvent].isInvalid shouldBe true
//    testProbe.expectMessage(event1)
//
//    subscription.unsubscribe().await
//
//    publisher.publish(event1).await
//    testProbe.expectNoMessage(2.seconds)
//  }
//}
