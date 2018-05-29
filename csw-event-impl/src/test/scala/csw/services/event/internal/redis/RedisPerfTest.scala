package csw.services.event.internal.redis

import akka.actor.ActorSystem
import com.github.sebruck.EmbeddedRedis
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.perf.EventServicePerfFramework
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.throttle.{RateAdapterStage, RateLimiterStage}
import csw.services.event.internal.wiring.{EventServiceResolver, Wiring}
import csw.services.event.scaladsl.RedisFactory
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

class RedisPerfTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis with MockitoSugar {
  private val redisPort = 6379
  private val redis     = RedisServer.builder().setting("bind 127.0.0.1").port(redisPort).build()

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private val wiring                            = new Wiring(actorSystem)
  import wiring._
  private val redisClient         = RedisClient.create()
  private val eventPublisherUtil  = new EventPublisherUtil()
  private val eventSubscriberUtil = new EventSubscriberUtil()
  val redisFactory                = new RedisFactory(redisClient, mock[EventServiceResolver], eventPublisherUtil, eventSubscriberUtil)

  private val publisher  = redisFactory.publisher("localhost", redisPort)
  private val subscriber = redisFactory.subscriber("localhost", redisPort)
  private val framework  = new EventServicePerfFramework(publisher, subscriber)

  override def beforeAll(): Unit = {
    redis.start()
  }

  override def afterAll(): Unit = {
    redisClient.shutdown()
    redis.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  ignore("limiter") {
    framework.comparePerf(new RateLimiterStage(_))
  }

  ignore("adapter") {
    framework.comparePerf(new RateAdapterStage(_))
  }

  ignore("throughput-latency") {
    framework.monitorPerf()
  }
}
