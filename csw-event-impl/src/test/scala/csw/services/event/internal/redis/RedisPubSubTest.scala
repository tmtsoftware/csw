package csw.services.event.internal.redis

import akka.actor.ActorSystem
import com.github.sebruck.EmbeddedRedis
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

class RedisPubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {

  private val actorSystem = ActorSystem()
  private val redisPort   = 6379
  private lazy val redis  = RedisServer.builder().setting("bind 127.0.0.1").port(redisPort).build()
  private val wiring      = new RedisWiring("localhost", redisPort, actorSystem)
  private val publisher   = wiring.publisher()

  private val framework = new EventServicePubSubTestFramework(wiring)

  override protected def beforeAll(): Unit = {
    redis.start()
  }

  override def afterAll(): Unit = {
    publisher.shutdown().await
    redis.stop()
    actorSystem.terminate().await
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
