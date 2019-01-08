package csw.event.client.internal.redis

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.event.api.exceptions.{EventServerNotAvailable, PublishFailure}
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.helpers.Utils
import csw.event.client.helpers.Utils.makeDistinctEvent
import csw.params.events.EventKey
import io.lettuce.core.ClientOptions.DisconnectedBehavior
import io.lettuce.core.{ClientOptions, RedisException}
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
//DEOPSCSW-399: Propagate failure for publish api when redis/kafka server is down
class RedisFailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val redisClientOptions = ClientOptions
    .builder()
    .autoReconnect(false)
    .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
    .build

  private val redisTestProps: RedisTestProps = RedisTestProps.createRedisProperties(clientOptions = redisClientOptions)

  override def beforeAll(): Unit = redisTestProps.start()

  override def afterAll(): Unit = redisTestProps.shutdown()

  test("should throw PublishFailed exception on publish failure") {
    import redisTestProps._
    val publisher = eventService.makeNewPublisher()
    publisher.publish(Utils.makeEvent(1)).await

    redisServer.stop()

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val failedEvent = Utils.makeEvent(2)
    val failure = intercept[PublishFailure] {
      publisher.publish(failedEvent).await
    }

    redisServer.start()

    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RedisException]
  }

  //DEOPSCSW-334: Publish an event
  test("should invoke onError callback on publish failure [stream API]") {
    import redisTestProps._
    val publisher = eventService.makeNewPublisher()
    val testProbe = TestProbe[PublishFailure]()(typedActorSystem)
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val event       = Utils.makeEvent(1)
    val eventStream = Source.single(event)

    publisher.publish(eventStream, failure ⇒ testProbe.ref ! failure)

    val failure = testProbe.expectMessageType[PublishFailure]
    failure.event shouldBe event
    failure.getCause shouldBe a[RedisException]
  }

  //DEOPSCSW-334: Publish an event
  test("should invoke onError callback on publish failure [eventGenerator API]") {
    import redisTestProps._
    val publisher = eventService.makeNewPublisher()
    val testProbe = TestProbe[PublishFailure]()(typedActorSystem)
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val event = Utils.makeEvent(1)

    publisher.publish(event, 20.millis, failure ⇒ testProbe.ref ! failure)

    val failure = testProbe.expectMessageType[PublishFailure]
    failure.event shouldBe event
    failure.getCause shouldBe a[RedisException]
  }

  test("should throw EventServerNotAvailable exception on subscription failure") {
    import redisTestProps._
    val event1             = makeDistinctEvent(Random.nextInt())
    val eventKey: EventKey = event1.eventKey

    redisServer.stop()

    intercept[EventServerNotAvailable] {
      val subscription = subscriber.subscribe(Set(eventKey)).toMat(Sink.foreach(println))(Keep.left).run()
      subscription.ready().await
    }

    redisServer.start()
  }

  //DEOPSCSW-000: Publish an event with block generating future of event
  test("should invoke onError callback on publish failure [eventGenerator API] with future of event generator") {
    import redisTestProps._
    val publisher = eventService.makeNewPublisher()
    val testProbe = TestProbe[PublishFailure]()(typedActorSystem)
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val event = Utils.makeEvent(1)

    publisher.publishAsync(Future.successful(event), 20.millis, failure ⇒ testProbe.ref ! failure)

    val failure = testProbe.expectMessageType[PublishFailure]
    failure.event shouldBe event
    failure.getCause shouldBe a[RedisException]
  }

}
