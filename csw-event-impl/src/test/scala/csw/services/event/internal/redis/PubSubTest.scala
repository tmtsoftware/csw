package csw.services.event.internal.redis

import akka.stream.scaladsl.{Keep, Sink, Source}
import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.PortHelper
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.Wiring
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class PubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {

  private val port: Port = PortHelper.freePort
  private val wiring     = new Wiring(port)

  import wiring._
  lazy val redis: RedisServer = RedisServer.builder().setting("bind 127.0.0.1").port(port).build()

  redis.start()

  override def afterAll(): Unit = {
    Await.result(redisClient.shutdownAsync().toCompletableFuture.toScala, 5.seconds)
    Await.result(actorSystem.terminate(), 5.seconds)
    redis.stop()
  }

  test("pub-sub") {
    val prefix             = Prefix("test.prefix")
    val eventName          = EventName("system")
    val event              = SystemEvent(prefix, eventName)
    val eventKey: EventKey = event.eventKey

    val (subscription, seqF) = subscriberImpl.subscribe(Seq(eventKey)).toMat(Sink.seq)(Keep.both).run()

    Thread.sleep(10)
    publisherImpl.publish(event).await
    subscription.unsubscribe().await
    seqF.await shouldBe Seq(event)
  }

  ignore("perf") {
    val prefix    = Prefix("test.prefix")
    val eventName = EventName("system")

    def event: Event = SystemEvent(prefix, eventName)

    val eventKey: EventKey = event.eventKey
    subscriberImpl.subscribe(Seq(eventKey)).runForeach { x =>
      val begin = x.eventTime.time.toEpochMilli
      println(System.currentTimeMillis() - begin)
    }

    Thread.sleep(10)

    Source
      .fromIterator(() => Iterator.continually(event))
      .mapAsync(1)(publisherImpl.publish)
      .runForeach(_ => ())
      .await
  }
}
