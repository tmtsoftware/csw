package csw.services.event.internal.redis

import akka.stream.scaladsl.Source
import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.services.event.exceptions.PublishFailure
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils
import io.lettuce.core.ClientOptions.DisconnectedBehavior
import io.lettuce.core.{ClientOptions, RedisException}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
class RedisFailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val redisClientOptions = ClientOptions
    .builder()
    .autoReconnect(false)
    .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
    .build

  private val redisTestProps: RedisTestProps = RedisTestProps.createRedisProperties(3560, 26380, 6380, redisClientOptions)

  override def beforeAll(): Unit = redisTestProps.start()

  override def afterAll(): Unit = redisTestProps.shutdown()

  test("should throw PublishFailed exception on publish failure") {
    import redisTestProps._
    val publisher = eventService.makeNewPublisher().await
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val failedEvent = Utils.makeEvent(2)
    val failure = intercept[PublishFailure] {
      publisher.publish(failedEvent).await
    }
    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RedisException]
  }

  //DEOPSCSW-334: Publish an event
  test("should invoke onError callback on publish failure [stream API]") {
    import redisTestProps._
    val publisher = eventService.makeNewPublisher().await
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
    val publisher = eventService.makeNewPublisher().await
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
}
