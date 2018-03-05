package csw.services.event.internal.redis

import akka.actor.ActorSystem
import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.Event
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.{EventServicePubSubTestFramework, RateAdapterStage, RateLimiterStage}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

class RedisPubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {

  private implicit val actorSystem: ActorSystem = ActorSystem()

  private val redisPort  = 6379
  private lazy val redis = RedisServer.builder().setting("bind 127.0.0.1").port(redisPort).build()
  private val wiring     = new RedisWiring("localhost", redisPort, actorSystem)
  private val publisher  = wiring.publisher()
  private val subscriber = wiring.subscriber()

  private val framework = new EventServicePubSubTestFramework(publisher, subscriber)

  override protected def beforeAll(): Unit = {
    redis.start()
  }

  override def afterAll(): Unit = {
    publisher.shutdown().await
    redis.stop()
    actorSystem.terminate().await
  }

  ignore("limiter") {
    framework.comparePerf(new RateLimiterStage(_))
  }

  ignore("adapter") {
    framework.comparePerf(new RateAdapterStage(_))
  }

  ignore("redis-throughput-latency") {
    framework.monitorPerf()
  }

  test("Redis pub sub") {
    framework.pubSub()
  }

  test("Redis independent subscriptions") {
    framework.subscribeIndependently()
  }

  ignore("Redis multiple publish") {
    framework.publishMultiple()
  }

  test("Redis retrieve recently published event on subscription") {
    framework.retrieveRecentlyPublished()
  }

  test("Redis retrieveInvalidEvent") {
    framework.retrieveInvalidEvent()
  }

  test("Redis get") {
    framework.get()
  }

  test("Redis get retrieveInvalidEvent") {
    framework.retrieveInvalidEventOnget()
  }

}
