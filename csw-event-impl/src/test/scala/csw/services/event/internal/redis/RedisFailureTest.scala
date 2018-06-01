package csw.services.event.internal.redis

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.Source
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailedException
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils
import io.lettuce.core.ClientOptions
import io.lettuce.core.ClientOptions.DisconnectedBehavior
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

  private val masterId = "mymaster"

  private val redisTestProps: RedisTestProps = RedisTestProps.createRedisProperties(3560, 26380, 6380, redisClientOptions)

  import redisTestProps._
  import redisTestProps.wiring._

  override def beforeAll(): Unit = {
    redisSentinel.start()
    redis.start()
  }

  override def afterAll(): Unit = {
    redisClient.shutdown()
    redis.stop()
    redisSentinel.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  test("should throw PublishFailed exception on publish failure") {
    val publisher = redisFactory.publisher(masterId).await

    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    intercept[PublishFailedException] {
      publisher.publish(Utils.makeEvent(2)).await
    }
  }

  //DEOPSCSW-334: Publish an event
  test("should invoke onError callback on publish failure [stream API]") {
    val publisher = redisFactory.publisher(masterId).await

    val testProbe = TestProbe[Event]()(actorSystem.toTyped)
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val event       = Utils.makeEvent(1)
    val eventStream = Source.single(event)

    publisher.publish(eventStream, event ⇒ testProbe.ref ! event)

    testProbe.expectMessage(event)
  }

  //DEOPSCSW-334: Publish an event
  test("should invoke onError callback on publish failure [eventGenerator API]") {
    val publisher = redisFactory.publisher(masterId).await

    val testProbe = TestProbe[Event]()(actorSystem.toTyped)
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val event = Utils.makeEvent(1)

    publisher.publish(event, 20.millis, event ⇒ testProbe.ref ! event)

    testProbe.expectMessage(event)
  }
}
