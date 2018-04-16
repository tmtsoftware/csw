package csw.services.event.internal.redis

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.Source
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailed
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.{RegistrationFactory, Utils}
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.event.scaladsl.RedisFactory
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import io.lettuce.core.ClientOptions.DisconnectedBehavior
import io.lettuce.core.{ClientOptions, RedisClient}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.concurrent.duration.DurationInt

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
class RedisFailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val seedPort        = 3560
  private val redisPort       = 6379
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, redisPort)
  locationService.register(tcpRegistration).await

  private val redis = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()

  private implicit val actorSystem: ActorSystem = clusterSettings.system

  private val redisClient = RedisClient.create()

  redisClient.setOptions(
    ClientOptions.builder().autoReconnect(false).disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS).build()
  )

  private val wiring       = new Wiring(actorSystem)
  private val redisFactory = new RedisFactory(redisClient, locationService, wiring)

  case class FailedEvent(event: Event, throwable: Throwable)

  override def beforeAll(): Unit = {
    redis.start()
  }

  override def afterAll(): Unit = {
    redisClient.shutdown()
    redis.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  test("failure in publishing should fail future with PublishFailed exception") {
    val publisher = redisFactory.publisher().await

    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    intercept[PublishFailed] {
      publisher.publish(Utils.makeEvent(2)).await
    }
  }

  test("handle failed publish event with a callback") {
    val publisher = redisFactory.publisher().await

    val testProbe = TestProbe[FailedEvent]()(actorSystem.toTyped)
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val event       = Utils.makeEvent(1)
    val eventStream = Source.single(event)

    publisher.publish(eventStream, (event, ex) ⇒ testProbe.ref ! FailedEvent(event, ex))

    val failedEvent = testProbe.expectMessageType[FailedEvent]

    failedEvent.event shouldBe event
    failedEvent.throwable shouldBe a[PublishFailed]
  }

  test("handle failed publish event with an eventGenerator and a callback") {
    val publisher = redisFactory.publisher().await

    val testProbe = TestProbe[FailedEvent]()(actorSystem.toTyped)
    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    Thread.sleep(1000) // wait till the publisher is shutdown successfully

    val event = Utils.makeEvent(1)

    publisher.publish(event, 20.millis, (event, ex) ⇒ testProbe.ref ! FailedEvent(event, ex))

    val failedEvent = testProbe.expectMessageType[FailedEvent]

    failedEvent.event shouldBe event
    failedEvent.throwable shouldBe a[PublishFailed]
  }
}
