package csw.services.event.internal.redis

import akka.actor.ActorSystem
import com.github.sebruck.EmbeddedRedis
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.events.EventKey
import csw.services.event.RedisFactory
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils.makeEvent
import csw.services.event.internal.EventServicePubSubTestFramework
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

class PubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {
  private val seedPort        = 3558
  private val redisPort       = 6379
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, redisPort)
  locationService.register(tcpRegistration).await

  private val redis = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()

  private implicit val actorSystem: ActorSystem = clusterSettings.system
  private val redisClient                       = RedisClient.create()
  private val wiring                            = new Wiring(actorSystem)
  private val redisFactory                      = new RedisFactory(redisClient, locationService, wiring)
  private val publisher                         = redisFactory.publisher().await
  private val subscriber                        = redisFactory.subscriber().await
  private val framework                         = new EventServicePubSubTestFramework(publisher, subscriber)

  override def beforeAll(): Unit = {
    redis.start()
  }

  override def afterAll(): Unit = {
    redisClient.shutdown()
    redis.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  // DEOPSCSW-334 : Publish an event
  test("Redis - should be able to publish and subscribe an event") {
    framework.pubSub()
  }

  test("Redis - should be able to make independent subscriptions") {
    framework.subscribeIndependently()
  }

  test("Redis - should be able to publish concurrently to the same channel") {
    framework.publishMultiple()
  }

  test("Redis - should be able to publish concurrently to the different channel") {
    framework.publishMultipleToDifferentChannels()
  }

  test("Redis - should be able to retrieve recently published event on subscription") {
    framework.retrieveRecentlyPublished()
  }

  test("Redis - should be able to retrieve InvalidEvent") {
    framework.retrieveInvalidEvent()
  }

  // DEOPSCSW-334 : Publish an event
  test("Redis - should be able to get an event without subscribing for it") {
    framework.get()
  }

  test("Redis - should be able to get InvalidEvent") {
    framework.retrieveInvalidEventOnGet()
  }

  // DEOPSCSW-334 : Publish an event
  test("Redis - should be able to persist the event in DB against the same key as channel name while publishing") {
    val event1             = makeEvent(1)
    val eventKey: EventKey = event1.eventKey
    val redisCommands =
      redisClient.connect(EventServiceCodec, RedisURI.create(clusterSettings.hostname, redisPort)).sync()

    publisher.publish(event1).await

    Thread.sleep(1000)
    redisCommands.get(eventKey) shouldBe event1
  }
}
